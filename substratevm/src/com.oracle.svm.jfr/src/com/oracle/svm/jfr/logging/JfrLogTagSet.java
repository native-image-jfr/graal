/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, 2021, Red Hat Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.svm.jfr.logging;

import java.util.Set;
import java.util.EnumSet;

/**
 * A JFR-related log tag set. This class is like {@link jdk.jfr.internal.LogTag}, but with added
 * functionality needed for {@link JfrLogConfiguration}.
 */
public enum JfrLogTagSet {
    JFR(0, JfrLogTag.JFR),
    JFR_SYSTEM(1, JfrLogTag.JFR, JfrLogTag.SYSTEM),
    JFR_SYSTEM_EVENT(2, JfrLogTag.JFR, JfrLogTag.SYSTEM, JfrLogTag.EVENT),
    JFR_SYSTEM_SETTING(3, JfrLogTag.JFR, JfrLogTag.SYSTEM, JfrLogTag.SETTING),
    JFR_SYSTEM_BYTECODE(4, JfrLogTag.JFR, JfrLogTag.SYSTEM, JfrLogTag.BYTECODE),
    JFR_SYSTEM_PARSER(5, JfrLogTag.JFR, JfrLogTag.SYSTEM, JfrLogTag.PARSER),
    JFR_SYSTEM_METADATA(6, JfrLogTag.JFR, JfrLogTag.SYSTEM, JfrLogTag.METADATA),
    JFR_METADATA(7, JfrLogTag.JFR, JfrLogTag.METADATA),
    JFR_EVENT(8, JfrLogTag.JFR, JfrLogTag.EVENT),
    JFR_SETTING(9, JfrLogTag.JFR, JfrLogTag.SETTING),
    JFR_DCMD(10, JfrLogTag.JFR, JfrLogTag.DCMD);

    private static final JfrLogTagSet[] IDMAP;
    private final int id; // Must match the id of the corresponding jdk.jfr.internal.LogTag
    private final Set<JfrLogTag> tags;
    private JfrLogLevel level;

    JfrLogTagSet(int id, JfrLogTag firstTag, JfrLogTag... restTags) {
        this.id = id;
        this.tags = EnumSet.of(firstTag, restTags);
    }

    static {
        IDMAP = new JfrLogTagSet[JfrLogTagSet.values().length];
        for (JfrLogTagSet jfrLogTagSet : JfrLogTagSet.values()) {
            IDMAP[jfrLogTagSet.id] = jfrLogTagSet;
        }
    }

    public static JfrLogTagSet fromTagSetId(int tagSetId) {
        return IDMAP[tagSetId];
    }

    public Set<JfrLogTag> getTags() {
        return tags;
    }

    public void setLevel(JfrLogLevel level) {
        this.level = level;
    }

    public JfrLogLevel getLevel() {
        return level;
    }
}
