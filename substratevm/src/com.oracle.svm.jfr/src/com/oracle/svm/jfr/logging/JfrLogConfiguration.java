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

import java.util.Set;
import java.util.EnumSet;
import java.util.Optional;

/**
 * Handles configuration for flight recorder logging.
 */
public final class JfrLogConfiguration {
    private static JfrLogSelection[] selections;

    private JfrLogConfiguration() {}

    public static boolean shouldLog(int tagSetId, int level) {
        Optional<LogLevel> tagSetLogLevel = JfrLogTagSet.fromTagSetId(tagSetId).getLevel();
        // LogLevel#level is not accessible, so have to use ordinal + 1
        return tagSetLogLevel.isEmpty() ? false : tagSetLogLevel.get().ordinal() + 1 <= level;
    }

    public static void parse(String config) {
        String[] splitConfig = config.split(",");
        selections = new JfrLogSelection[splitConfig.length];

        int index = 0;
        for (String s : splitConfig) {
            JfrLogSelection selection = new JfrLogSelection();
            selection.parse(s);
            selections[index++] = selection;
        }
        setLogTagSetLevels();
    }

    private static void setLogTagSetLevels() {
        for (JfrLogTagSet tagSet : JfrLogTagSet.values()) {
            Optional<LogLevel> level = Optional.empty();
            for (JfrLogSelection selection : selections) {
                if ((selection.wildcard && tagSet.getTags().containsAll(selection.tags))
                        || (!selection.wildcard && selection.tags.equals(tagSet.getTags()))) {
                    level = Optional.of(selection.level);
                }
            }
            tagSet.setLevel(level);
        }
    }

    private static class JfrLogSelection {
        private Set<JfrLogTag> tags;
        private LogLevel level = LogLevel.INFO;
        private boolean all = false;
        private boolean wildcard = false;

        private void parse(String str) {
            int equalsIndex;
            if ((equalsIndex = str.indexOf('=')) > 0) {
                level = LogLevel.valueOf(str.substring(equalsIndex + 1));
                str = str.substring(0, equalsIndex);
            }

            if (str.equals("all")) {
                all = true;
                tags = EnumSet.allOf(JfrLogTag.class);
            }

            if (str.indexOf('*') > 0) {
                wildcard = true;
                str = str.substring(0, str.length() - 1);
            }

            tags  = EnumSet.noneOf(JfrLogTag.class);
            for (String s : str.split("\\+")) {
                tags.add(JfrLogTag.valueOf(s));
            }
        }
    }
}
