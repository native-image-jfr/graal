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
import com.oracle.svm.core.util.UserError;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Handles configuration for flight recorder logging.
 */
public enum JfrLogConfiguration {
    INSTANCE;

    private boolean loggingEnabled = false;
    private JfrLogSelection[] selections;

    public boolean shouldLog(int tagSetId, int level) {
        if (!loggingEnabled) {
            return false;
        }
        Target_jdk_jfr_internal_LogLevel tagSetLogLevel = JfrLogTagSet.fromTagSetId(tagSetId).getLevel();
        return tagSetLogLevel == null ? false : tagSetLogLevel.level <= level;
    }

    public void parse(String config) {
        if (config.isBlank()) {
            return;
        } else if (config.equalsIgnoreCase("help")) {
            printHelp();
            System.exit(0);
        }

        loggingEnabled = true;

        String[] splitConfig = config.split(",");
        selections = new JfrLogSelection[splitConfig.length];

        int index = 0;
        for (String s : splitConfig) {
            selections[index++] = JfrLogSelection.parse(s);
        }
        setLogTagSetLevels();
        verifySelections();
    }

    private void setLogTagSetLevels() {
        for (JfrLogTagSet tagSet : JfrLogTagSet.values()) {
            Target_jdk_jfr_internal_LogLevel level = null;
            for (JfrLogSelection selection : selections) {
                if ((selection.wildcard && tagSet.getTags().containsAll(selection.tags))
                        || (selection.tags.equals(tagSet.getTags()))) {
                    level = selection.level;
                    selection.matchesATagSet = true;
                }
            }
            tagSet.setLevel(level);
        }
    }

    private void verifySelections() {
        for (JfrLogSelection selection : selections) {
            if (!selection.matchesATagSet) {
                throw UserError.abort("No tag set matches tag combination %s for FlightRecorderLogging",
                        selection.tags.toString().toLowerCase() + (selection.wildcard ? "*" : ""));
            }
        }
    }

    private static class JfrLogSelection {
        private final Set<JfrLogTag> tags;
        private final Target_jdk_jfr_internal_LogLevel level;
        private final boolean wildcard;
        private boolean matchesATagSet = false;

        JfrLogSelection(Set<JfrLogTag> tags, Target_jdk_jfr_internal_LogLevel level, boolean wildcard) {
            this.tags = tags;
            this.level = level;
            this.wildcard = wildcard;
        }

        private static JfrLogSelection parse(String str) {
            Set<JfrLogTag> tags = EnumSet.noneOf(JfrLogTag.class);
            Target_jdk_jfr_internal_LogLevel level = Target_jdk_jfr_internal_LogLevel.INFO;
            boolean wildcard = false;

            String tagsStr;
            int equalsIndex;
            if ((equalsIndex = str.indexOf('=')) > 0) {
                try {
                    level = Target_jdk_jfr_internal_LogLevel.valueOf(str.substring(equalsIndex + 1).toUpperCase());
                } catch (IllegalArgumentException | NullPointerException e) {
                    throw UserError.abort(e, "Invalid log level '%s' for FlightRecorderLogging. Use -XX:FlightRecorderLogging=help to see help.",
                            str.substring(equalsIndex + 1));
                }
                tagsStr = str.substring(0, equalsIndex);
            } else {
                tagsStr = str;
            }

            if (tagsStr.equalsIgnoreCase("all")) {
                return new JfrLogSelection(tags, level, true);
            }

            if (tagsStr.endsWith("*")) {
                wildcard = true;
                tagsStr = tagsStr.substring(0, tagsStr.length() - 1);
            }

            for (String s : tagsStr.split("\\+")) {
                try {
                    tags.add(JfrLogTag.valueOf(s.toUpperCase()));
                } catch (IllegalArgumentException | NullPointerException e) {
                    throw UserError.abort(e, "Invalid log tag '%s' for FlightRecorderLogging. Use -XX:FlightRecorderLogging=help to see help.", s);
                }
            }
            return new JfrLogSelection(tags, level, wildcard);
        }
    }

    private void printHelp() {
        Log log = Log.log();
        log.string("Usage: -XX:FlightRecorderLogging=[tag1[+tag2...][*][=level][,...]]").newline();
        log.string("When this option is not set, logging is disabled.").newline();
        log.string("When this option is set, logging will be enabled for messages with tag sets that match the given tag combinations, at the specified levels.").newline();
        log.string("If a tag set does not have a matching tag combination from this option, then logging for that tag set is disabled.").newline();
        log.string("Unless wildcard (*) is specified, only log messages tagged with exactly the tags specified will be matched.").newline();
        log.string("Specifying 'all' instead of a tag combination matches all tag combinations.").newline();
        log.string("A tag combination without a log level is given a default log level of INFO.").newline();
        log.string("If more than one tag combination applies to the same tag set, the rightmost one will be used.").newline();
        log.string("This option is case insensitive.").newline();
        log.string("Available log levels:").newline().string(Arrays.toString(Target_jdk_jfr_internal_LogLevel.values()).toLowerCase()).newline();
        log.string("Available log tags:").newline().string(Arrays.toString(JfrLogTag.values()).toLowerCase()).newline();
    }
}
