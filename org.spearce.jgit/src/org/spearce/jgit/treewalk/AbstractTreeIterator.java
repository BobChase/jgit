/*
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Git Development Community nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spearce.jgit.treewalk;

import java.io.IOException;

import org.spearce.jgit.errors.CorruptObjectException;
import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.FileMode;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.treewalk.filter.TreeFilter;

/**
 * Walks a Git tree (directory) in Git sort order.
 * <p>
 * A new iterator instance should be positioned before the first entry. The data
 * about the first entry is not available until after the first call to
 * {@link #next()} is made.
 * <p>
 * Implementors must walk a tree in the Git sort order, which has the following
 * odd sorting:
 * <ol>
 * <li>A.c</li>
 * <li>A/c</li>
 * <li>A0c</li>
 * </ol>
 * <p>
 * In the second item, <code>A</code> is the name of a subtree and
 * <code>c</code> is a file within that subtree. The other two items are files
 * in the root level tree.
 * 
 * @see CanonicalTreeParser
 */
public abstract class AbstractTreeIterator {
	private static final int DEFAULT_PATH_SIZE = 128;

	/** A dummy object id buffer that matches the zero ObjectId. */
	protected static final byte[] zeroid = new byte[Constants.OBJECT_ID_LENGTH];

	/** Iterator for the parent tree; null if we are the root iterator. */
	final AbstractTreeIterator parent;

	/** The iterator this current entry is path equal to. */
	AbstractTreeIterator matches;

	/**
	 * Mode bits for the current entry.
	 * <p>
	 * A numerical value from FileMode is usually faster for an iterator to
	 * obtain from its data source so this is the preferred representation.
	 * 
	 * @see org.spearce.jgit.lib.FileMode
	 */
	protected int mode;

	/**
	 * Path buffer for the current entry.
	 * <p>
	 * This buffer is pre-allocated at the start of walking and is shared from
	 * parent iterators down into their subtree iterators. The sharing allows
	 * the current entry to always be a full path from the root, while each
	 * subtree only needs to populate the part that is under their control.
	 */
	protected byte[] path;

	/**
	 * Position within {@link #path} this iterator starts writing at.
	 * <p>
	 * This is the first offset in {@link #path} that this iterator must
	 * populate during {@link #next}. At the root level (when {@link #parent}
	 * is null) this is 0. For a subtree iterator the index before this position
	 * should have the value '/'.
	 */
	protected final int pathOffset;

	/**
	 * Total length of the current entry's complete path from the root.
	 * <p>
	 * This is the number of bytes within {@link #path} that pertain to the
	 * current entry. Values at this index through the end of the array are
	 * garbage and may be randomly populated from prior entries.
	 */
	protected int pathLen;

	/** Create a new iterator with no parent. */
	protected AbstractTreeIterator() {
		parent = null;
		path = new byte[DEFAULT_PATH_SIZE];
		pathOffset = 0;
	}

	/**
	 * Create an iterator for a subtree of an existing iterator.
	 * 
	 * @param p
	 *            parent tree iterator.
	 */
	protected AbstractTreeIterator(final AbstractTreeIterator p) {
		parent = p;
		path = p.path;
		pathOffset = p.pathLen + 1;
		try {
			path[pathOffset - 1] = '/';
		} catch (ArrayIndexOutOfBoundsException e) {
			growPath(p.pathLen);
			path[pathOffset - 1] = '/';
		}
	}

	/**
	 * Grow the path buffer larger.
	 * 
	 * @param len
	 *            number of live bytes in the path buffer. This many bytes will
	 *            be moved into the larger buffer.
	 */
	protected void growPath(final int len) {
		final byte[] n = new byte[path.length << 1];
		System.arraycopy(path, 0, n, 0, len);
		for (AbstractTreeIterator p = this; p != null; p = p.parent)
			p.path = n;
	}

	/**
	 * Compare the path of this current entry to another iterator's entry.
	 * 
	 * @param p
	 *            the other iterator to compare the path against.
	 * @return -1 if this entry sorts first; 0 if the entries are equal; 1 if
	 *         p's entry sorts first.
	 */
	public int pathCompare(final AbstractTreeIterator p) {
		final byte[] a = path;
		final byte[] b = p.path;
		final int aLen = pathLen;
		final int bLen = p.pathLen;
		int cPos;

		for (cPos = 0; cPos < aLen && cPos < bLen; cPos++) {
			final int cmp = (a[cPos] & 0xff) - (b[cPos] & 0xff);
			if (cmp != 0)
				return cmp;
		}

		if (cPos < aLen) {
			final int aj = a[cPos] & 0xff;
			final int lastb = p.lastPathChar();
			if (aj < lastb)
				return -1;
			else if (aj > lastb)
				return 1;
			else if (cPos == aLen - 1)
				return 0;
			else
				return -1;
		}

		if (cPos < bLen) {
			final int bk = b[cPos] & 0xff;
			final int lasta = lastPathChar();
			if (lasta < bk)
				return -1;
			else if (lasta > bk)
				return 1;
			else if (cPos == bLen - 1)
				return 0;
			else
				return 1;
		}

		final int lasta = lastPathChar();
		final int lastb = p.lastPathChar();
		if (lasta < lastb)
			return -1;
		else if (lasta > lastb)
			return 1;

		if (aLen == bLen)
			return 0;
		else if (aLen < bLen)
			return -1;
		else
			return 1;
	}

	private int lastPathChar() {
		return FileMode.TREE.equals(mode) ? '/' : '\0';
	}

	/**
	 * Check if the current entry of both iterators has the same id.
	 * <p>
	 * This method is faster than {@link #getEntryObjectId()} as it does not
	 * require copying the bytes out of the buffers. A direct {@link #idBuffer}
	 * compare operation is performed.
	 * 
	 * @param otherIterator
	 *            the other iterator to test against.
	 * @return true if both iterators have the same object id; false otherwise.
	 */
	public boolean idEqual(final AbstractTreeIterator otherIterator) {
		return ObjectId.equals(idBuffer(), idOffset(),
				otherIterator.idBuffer(), otherIterator.idOffset());
	}

	/**
	 * Get the object id of the current entry.
	 * 
	 * @return an object id for the current entry.
	 */
	public ObjectId getEntryObjectId() {
		return ObjectId.fromRaw(idBuffer(), idOffset());
	}

	/**
	 * Get the byte array buffer object IDs must be copied out of.
	 * <p>
	 * The id buffer contains the bytes necessary to construct an ObjectId for
	 * the current entry of this iterator. The buffer can be the same buffer for
	 * all entries, or it can be a unique buffer per-entry. Implementations are
	 * encouraged to expose their private buffer whenever possible to reduce
	 * garbage generation and copying costs.
	 * 
	 * @return byte array the implementation stores object IDs within.
	 * @see #getEntryObjectId()
	 */
	protected abstract byte[] idBuffer();

	/**
	 * Get the position within {@link #idBuffer()} of this entry's ObjectId.
	 * 
	 * @return offset into the array returned by {@link #idBuffer()} where the
	 *         ObjectId must be copied out of.
	 */
	protected abstract int idOffset();

	/**
	 * Create a new iterator for the current entry's subtree.
	 * <p>
	 * The parent reference of the iterator must be <code>this</code>,
	 * otherwise the caller would not be able to exit out of the subtree
	 * iterator correctly and return to continue walking <code>this</code>.
	 * 
	 * @param repo
	 *            repository to load the tree data from.
	 * @return a new parser that walks over the current subtree.
	 * @throws IncorrectObjectTypeException
	 *             the current entry is not actually a tree and cannot be parsed
	 *             as though it were a tree.
	 * @throws IOException
	 *             a loose object or pack file could not be read.
	 */
	public abstract AbstractTreeIterator createSubtreeIterator(Repository repo)
			throws IncorrectObjectTypeException, IOException;

	/**
	 * Is this tree iterator at its EOF point (no more entries)?
	 * <p>
	 * An iterator is at EOF if there is no current entry.
	 * 
	 * @return true if we have walked all entries and have none left.
	 */
	public abstract boolean eof();

	/**
	 * Advance to the next tree entry, populating this iterator with its data.
	 * <p>
	 * Implementations must populate the following members:
	 * <ul>
	 * <li>{@link #mode}</li>
	 * <li>{@link #path} (from {@link #pathOffset} to {@link #pathLen})</li>
	 * <li>{@link #pathLen}</li>
	 * </ul>
	 * as well as any implementation dependent information necessary to
	 * accurately return data from {@link #idBuffer()} and {@link #idOffset()}
	 * when demanded.
	 * 
	 * @throws CorruptObjectException
	 *             the tree is invalid.
	 */
	public abstract void next() throws CorruptObjectException;

	/**
	 * Advance to the next tree entry, populating this iterator with its data.
	 * <p>
	 * This method behaves like {@link #next()} but is called by
	 * {@link TreeWalk} only if a {@link TreeFilter} was used and ruled out the
	 * current entry from the results. In such cases this tree iterator may
	 * perform special behavior.
	 *
	 * @throws CorruptObjectException
	 *             the tree is invalid.
	 */
	public void skip() throws CorruptObjectException {
		next();
	}
}
