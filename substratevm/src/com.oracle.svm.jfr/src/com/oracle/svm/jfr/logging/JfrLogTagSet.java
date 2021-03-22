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

import jdk.jfr.internal.LogLevel;
import jdk.jfr.internal.LogTag;

import java.util.Arrays;
import java.util.Set;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

/**
 * A JFR-related log tag set. There is one for each value in jdk.jfr.internal.LogTag. {@code id}
 * must match with the {@code id} of the corresponding {@link jdk.jfr.internal.LogTag}.
 */
enum JfrLogTagSet {
    JFR(0, JfrLogTag.JFR),
    JFR_SYSTEM(1, JfrLogTag.JFR, JfrLogTag.SYSTEM),
    JFR_SYSTEM_EVENT(2, JfrLogTag.JFR, JfrLogTag.SYSTEM, JfrLogTag.EVENT),
    JFR_SYSTEM_SETTING(3, JfrLogTag.JFR, JfrLogTag.SYSTEM, JfrLogTag.SETTING),
    JFR_SYSTEM_BYTECODE(4, JfrLogTag.JFR, JfrLogTag.SYSTEM, JfrLogTag.BYTECODE),
    JFR_SYSTEM_PARSER(5, JfrLogTag.JFR, JfrLogTag.SYSTEM, JfrLogTag.PARSER),
    JFR_SYSTEM_METADATA(6, JfrLogTag.JFR, JfrLogTag.SYSTEM, JfrLogTag.METADATA),
    JFR_SYSTEM_STREAMING(7, JfrLogTag.JFR, JfrLogTag.SYSTEM, JfrLogTag.STREAMING),
    JFR_SYSTEM_THROTTLE(8, JfrLogTag.JFR, JfrLogTag.SYSTEM, JfrLogTag.THROTTLE),
    JFR_METADATA(9, JfrLogTag.JFR, JfrLogTag.METADATA),
    JFR_EVENT(10, JfrLogTag.JFR, JfrLogTag.EVENT),
    JFR_SETTING(11, JfrLogTag.JFR, JfrLogTag.SETTING),
    JFR_DCMD(12, JfrLogTag.JFR, JfrLogTag.DCMD);

    private final int id;
    private final Set<JfrLogTag> tags;
    private Optional<LogLevel> level; // Empty = do not log
    private static final Map<Integer, JfrLogTagSet> IDMAP = 
        new HashMap<>((int) (JfrLogTagSet.values().length / 0.75) + 1);

    private JfrLogTagSet(int id, JfrLogTag... tags) {
        this.id = id;
        this.tags = EnumSet.copyOf(Arrays.asList(tags));
    }

    static {
        for (JfrLogTagSet jfrLogTagSet : JfrLogTagSet.values()) {
            IDMAP.put(jfrLogTagSet.id, jfrLogTagSet);
        }
    }

    public static JfrLogTagSet fromTagSetId(int tagSetId) {
        return IDMAP.get(tagSetId);
    }

    public void setLevel(Optional<LogLevel> level) {
        this.level = level;
    }

    public Optional<LogLevel> getLevel() {
        return level;
    }

    public Set<JfrLogTag> getTags() {
        return tags;
    }
}
