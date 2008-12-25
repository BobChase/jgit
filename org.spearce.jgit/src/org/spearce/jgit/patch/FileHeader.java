import static org.spearce.jgit.util.RawParseUtils.decodeNoFallback;
import static org.spearce.jgit.util.RawParseUtils.extractBinaryString;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import org.spearce.jgit.util.RawParseUtils;
import org.spearce.jgit.util.TemporaryBuffer;
	protected static final byte[] DELETED_FILE_MODE = encodeASCII("deleted file mode ");
	protected static final byte[] NEW_FILE_MODE = encodeASCII("new file mode ");
	protected static final byte[] INDEX = encodeASCII("index ");
	protected FileMode newMode;
	protected ChangeType changeType;
	protected AbbreviatedObjectId newId;
	/** If {@link #patchType} is {@link PatchType#GIT_BINARY}, the new image */
	BinaryHunk forwardBinaryHunk;

	/** If {@link #patchType} is {@link PatchType#GIT_BINARY}, the old image */
	BinaryHunk reverseBinaryHunk;

	int getParentCount() {
		return 1;
	}

	/** @return the byte array holding this file's patch script. */
	public byte[] getBuffer() {
		return buf;
	}

	/** @return offset the start of this file's script in {@link #getBuffer()}. */
	public int getStartOffset() {
		return startOffset;
	}

	/** @return offset one past the end of the file script. */
	public int getEndOffset() {
		return endOffset;
	}

	/**
	 * Convert the patch script for this file into a string.
	 * <p>
	 * The default character encoding ({@link Constants#CHARSET}) is assumed for
	 * both the old and new files.
	 *
	 * @return the patch script, as a Unicode string.
	 */
	public String getScriptText() {
		return getScriptText(null, null);
	}

	/**
	 * Convert the patch script for this file into a string.
	 *
	 * @param oldCharset
	 *            hint character set to decode the old lines with.
	 * @param newCharset
	 *            hint character set to decode the new lines with.
	 * @return the patch script, as a Unicode string.
	 */
	public String getScriptText(Charset oldCharset, Charset newCharset) {
		return getScriptText(new Charset[] { oldCharset, newCharset });
	}

	protected String getScriptText(Charset[] charsetGuess) {
		if (getHunks().isEmpty()) {
			// If we have no hunks then we can safely assume the entire
			// patch is a binary style patch, or a meta-data only style
			// patch. Either way the encoding of the headers should be
			// strictly 7-bit US-ASCII and the body is either 7-bit ASCII
			// (due to the base 85 encoding used for a BinaryHunk) or is
			// arbitrary noise we have chosen to ignore and not understand
			// (e.g. the message "Binary files ... differ").
			//
			return extractBinaryString(buf, startOffset, endOffset);
		}

		if (charsetGuess != null && charsetGuess.length != getParentCount() + 1)
			throw new IllegalArgumentException("Expected "
					+ (getParentCount() + 1) + " character encoding guesses");

		if (trySimpleConversion(charsetGuess)) {
			Charset cs = charsetGuess != null ? charsetGuess[0] : null;
			if (cs == null)
				cs = Constants.CHARSET;
			try {
				return decodeNoFallback(cs, buf, startOffset, endOffset);
			} catch (CharacterCodingException cee) {
				// Try the much slower, more-memory intensive version which
				// can handle a character set conversion patch.
			}
		}

		final StringBuilder r = new StringBuilder(endOffset - startOffset);

		// Always treat the headers as US-ASCII; Git file names are encoded
		// in a C style escape if any character has the high-bit set.
		//
		final int hdrEnd = getHunks().get(0).getStartOffset();
		for (int ptr = startOffset; ptr < hdrEnd;) {
			final int eol = Math.min(hdrEnd, nextLF(buf, ptr));
			r.append(extractBinaryString(buf, ptr, eol));
			ptr = eol;
		}

		final String[] files = extractFileLines(charsetGuess);
		final int[] offsets = new int[files.length];
		for (final HunkHeader h : getHunks())
			h.extractFileLines(r, files, offsets);
		return r.toString();
	}

	private static boolean trySimpleConversion(final Charset[] charsetGuess) {
		if (charsetGuess == null)
			return true;
		for (int i = 1; i < charsetGuess.length; i++) {
			if (charsetGuess[i] != charsetGuess[0])
				return false;
		}
		return true;
	}

	private String[] extractFileLines(final Charset[] csGuess) {
		final TemporaryBuffer[] tmp = new TemporaryBuffer[getParentCount() + 1];
		try {
			for (int i = 0; i < tmp.length; i++)
				tmp[i] = new TemporaryBuffer();
			for (final HunkHeader h : getHunks())
				h.extractFileLines(tmp);

			final String[] r = new String[tmp.length];
			for (int i = 0; i < tmp.length; i++) {
				Charset cs = csGuess != null ? csGuess[i] : null;
				if (cs == null)
					cs = Constants.CHARSET;
				r[i] = RawParseUtils.decode(cs, tmp[i].toByteArray());
			}
			return r;
		} catch (IOException ioe) {
			throw new RuntimeException("Cannot convert script to text", ioe);
		} finally {
			for (final TemporaryBuffer b : tmp) {
				if (b != null)
					b.destroy();
			}
		}
	}

	public List<? extends HunkHeader> getHunks() {
	HunkHeader newHunkHeader(final int offset) {
		return new HunkHeader(this, offset);
	}

	/** @return if a {@link PatchType#GIT_BINARY}, the new-image delta/literal */
	public BinaryHunk getForwardBinaryHunk() {
		return forwardBinaryHunk;
	}

	/** @return if a {@link PatchType#GIT_BINARY}, the old-image delta/literal */
	public BinaryHunk getReverseBinaryHunk() {
		return reverseBinaryHunk;
	}

	 * @param end
	 *            one past the last position to parse.
	int parseGitFileName(int ptr, final int end) {
		if (eol >= end) {
	int parseGitHeaders(int ptr, final int end) {
		while (ptr < end) {
			if (isHunkHdr(buf, ptr, eol) >= 1) {
				parseOldName(ptr, eol);
				parseNewName(ptr, eol);
				newMode = FileMode.MISSING;
				parseNewFileMode(ptr, eol);
	protected void parseOldName(int ptr, final int eol) {
		oldName = p1(parseName(oldName, ptr + OLD_NAME.length, eol));
		if (oldName == DEV_NULL)
			changeType = ChangeType.ADD;
	}

	protected void parseNewName(int ptr, final int eol) {
		newName = p1(parseName(newName, ptr + NEW_NAME.length, eol));
		if (newName == DEV_NULL)
			changeType = ChangeType.DELETE;
	}

	protected void parseNewFileMode(int ptr, final int eol) {
		oldMode = FileMode.MISSING;
		newMode = parseFileMode(ptr + NEW_FILE_MODE.length, eol);
		changeType = ChangeType.ADD;
	}

	int parseTraditionalHeaders(int ptr, final int end) {
		while (ptr < end) {
			if (isHunkHdr(buf, ptr, eol) >= 1) {
				parseOldName(ptr, eol);
				parseNewName(ptr, eol);
	protected FileMode parseFileMode(int ptr, final int end) {
	protected void parseIndexLine(int ptr, final int end) {

	/**
	 * Determine if this is a patch hunk header.
	 *
	 * @param buf
	 *            the buffer to scan
	 * @param start
	 *            first position in the buffer to evaluate
	 * @param end
	 *            last position to consider; usually the end of the buffer (
	 *            <code>buf.length</code>) or the first position on the next
	 *            line. This is only used to avoid very long runs of '@' from
	 *            killing the scan loop.
	 * @return the number of "ancestor revisions" in the hunk header. A
	 *         traditional two-way diff ("@@ -...") returns 1; a combined diff
	 *         for a 3 way-merge returns 3. If this is not a hunk header, 0 is
	 *         returned instead.
	 */
	static int isHunkHdr(final byte[] buf, final int start, final int end) {
		int ptr = start;
		while (ptr < end && buf[ptr] == '@')
			ptr++;
		if (ptr - start < 2)
			return 0;
		if (ptr == end || buf[ptr++] != ' ')
			return 0;
		if (ptr == end || buf[ptr++] != '-')
			return 0;
		return (ptr - 3) - start;
	}