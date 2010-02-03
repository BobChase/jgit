/*
 * Copyright (C) 2010, Google Inc.
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
 * - Neither the name of the Eclipse Foundation, Inc. nor the
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

package org.eclipse.jgit.iplog;

import java.util.Comparator;
import java.util.Date;

/** A single contribution by a {@link Contributor}. */
class SingleContribution {
	/** Sorts contributors by their name first name, then last name. */
	public static final Comparator<SingleContribution> COMPARATOR = new Comparator<SingleContribution>() {
		public int compare(SingleContribution a, SingleContribution b) {
			return a.created.compareTo(b.created);
		}
	};

	private final String id;

	private String summary;

	private Date created;

	private String bugId;

	private String size;

	/**
	 * @param id
	 * @param created
	 * @param summary
	 */
	SingleContribution(String id, Date created, String summary) {
		this.id = id;
		this.summary = summary;
		this.created = created;
	}

	/** @return unique identity of the contribution. */
	String getID() {
		return id;
	}

	/** @return date the contribution was created. */
	Date getCreated() {
		return created;
	}

	/** @return summary of the contribution. */
	String getSummary() {
		return summary;
	}

	/** @return Bugzilla bug id */
	String getBugID() {
		return bugId;
	}

	void setBugID(String id) {
		if (id.startsWith("https://bugs.eclipse.org/"))
			id = id.substring("https://bugs.eclipse.org/".length());
		bugId = id;
	}

	String getSize() {
		return size;
	}

	void setSize(String sz) {
		size = sz;
	}
}