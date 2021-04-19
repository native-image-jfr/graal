/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.svm.jfr;

import com.oracle.svm.core.annotate.Uninterruptible;

/**
 * The event IDs depend on the metadata.xml and therefore vary between JDK versions.
 */
public enum JfrEvents {
    // TODO: we need to abstract the JDK version in some way.
    // Event IDs should be fetched similar to how we do it in JfrTypes.
    MetadataEvent(0),
    CheckpointEvent(1),
    ThreadStartEvent(255),
    ThreadEndEvent(256),
    DataLossEvent(335);

    private final int id;

    JfrEvents(int id) {
        this.id = id;
    }

    @Uninterruptible(reason = "Called from uninterruptible code.", mayBeInlined = true)
    public int getId() {
        return id;
    }

    public static int getEventCount() {
        // TODO: needs to return the count of all native events that are defined in the metadata.xml
        // file. The highest id must match "eventCount - 1".
        return 400;
    }
}
