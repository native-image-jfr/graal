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

import com.oracle.svm.core.log.Log;

import jdk.jfr.internal.LogLevel;

import java.util.Set;
import java.util.EnumSet;
import java.util.Optional;

/**
 * Handles configuration for flight recorder logging.
 */
public final class JfrLogConfiguration {
    private static JfrLogSelection[] selections;
    private static boolean loggingEnabled = false;

    private JfrLogConfiguration() {}

    public static boolean shouldLog(int tagSetId, int level) {
        if (!loggingEnabled) {
            return false;
        }
        Optional<LogLevel> tagSetLogLevel = JfrLogTagSet.fromTagSetId(tagSetId).getLevel();
        // LogLevel#level is not accessible, so we have to use ordinal + 1
        return tagSetLogLevel.isEmpty() ? false : tagSetLogLevel.get().ordinal() + 1 <= level;
    }

    public static void parse(String config) {
        if (config.isBlank()) {
            return;
        }
        loggingEnabled = true;

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
        private Set<JfrLogTag> tags = EnumSet.noneOf(JfrLogTag.class);
        private LogLevel level = LogLevel.INFO;
        private boolean wildcard = false;

        private void parse(String str) {
            int equalsIndex;
            if ((equalsIndex = str.indexOf('=')) > 0) {
                try {
                    level = LogLevel.valueOf(str.substring(equalsIndex + 1).toUpperCase());
                } catch (IllegalArgumentException | NullPointerException e) {
                    Log.logStream().println("error: Invalid log level in FlightRecorderLogging "
                            + str.substring(equalsIndex + 1));
                    System.exit(1);
                }
                str = str.substring(0, equalsIndex);
            }

            if (str.toUpperCase().equals("ALL")) {
                wildcard = true;
                return;
            }

            if (str.endsWith("*")) {
                wildcard = true;
                str = str.substring(0, str.length() - 1);
            }

            for (String s : str.split("\\+")) {
                try {
                    tags.add(JfrLogTag.valueOf(s.toUpperCase()));
                } catch (IllegalArgumentException | NullPointerException e) {
                    Log.logStream().println("error: Invalid log tag in FlightRecorderLogging " + s);
                    System.exit(1);
                }
            }
        }
    }
}
