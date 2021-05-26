/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2021, Red Hat Inc. All rights reserved.
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

package com.oracle.svm.jfr.traceid;

import com.oracle.svm.core.annotate.Uninterruptible;
import jdk.jfr.internal.Type;
import org.graalvm.compiler.api.replacements.Fold;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;

/**
 * The unique trace id for class constant pool entries. When classes are referenced in events, a unique id is tagged
 * as in-use for the current epoch. When the current chunk is closed the class constants and their child data
 * (classloader, module, package, etc.) that are tagged will be emitted as part of the CheckpointEvent so references
 * to the class via the unique trace id can be read
 *
 * The trace id uses internal bits to hold data about the tagged class
 *
 * @see com.oracle.svm.jfr.JfrTypeRepository for writing of the types constant pool entries
 */
public class JfrTraceId {

    public static final long BIT = 1;
    public static final long META_SHIFT = 8;
    public static final long TRANSIENT_META_BIT = (BIT << 3);
    public static final long TRANSIENT_BIT = (TRANSIENT_META_BIT << META_SHIFT);
    public static final long SERIALIZED_META_BIT = (BIT << 4);
    public static final long SERIALIZED_BIT = (SERIALIZED_META_BIT << META_SHIFT);

    private static final int TRACE_ID_SHIFT = 16;

    private static final long JDK_JFR_EVENT_SUBCLASS = 16;
    private static final long JDK_JFR_EVENT_CLASS = 32;
    private static final long EVENT_HOST_KLASS = 64;

    @Fold
    static JfrTraceIdMap getTraceIdMap() {
        return ImageSingletons.lookup(JfrTraceIdMap.class);
    }

    @Uninterruptible(reason = "Called from uninterruptible code.", mayBeInlined = true)
    public static void tag(Class<?> clazz, long bits) {
        JfrTraceIdMap map = getTraceIdMap();
        long id = map.getId(clazz);
        map.setId(clazz, id | (bits & 0xff));
    }

    @Uninterruptible(reason = "Called from uninterruptible code.", mayBeInlined = true)
    public static boolean predicate(Class<?> clazz, long bits) {
        JfrTraceIdMap map = getTraceIdMap();
        long id = map.getId(clazz);
        return (id & bits) != 0;
    }

    @Uninterruptible(reason = "Epoch must not change.")
    public static void setUsedThisEpoch(Class<?> clazz) {
        tag(clazz, JfrTraceIdEpoch.getInstance().thisEpochBit());
    }

    @Uninterruptible(reason = "Epoch must not change.")
    public static void clearUsedThisEpoch(Class<?> clazz, boolean epoch) {
        long bits = epoch ? JfrTraceIdEpoch.EPOCH_1_BIT : JfrTraceIdEpoch.EPOCH_0_BIT;
        JfrTraceIdMap map = getTraceIdMap();
        long id = map.getId(clazz);
        map.setId(clazz, id & ~bits);
    }

    @Uninterruptible(reason = "Epoch must not change.")
    public static boolean isUsedThisEpoch(Class<?> clazz) {
        return predicate(clazz, TRANSIENT_BIT | JfrTraceIdEpoch.getInstance().thisEpochBit());
    }

    @Uninterruptible(reason = "Called from uninterruptible code.", mayBeInlined = true)
    public static long getTraceIdRaw(Class<?> clazz) {
        return getTraceIdMap().getId(clazz);
    }

    @Uninterruptible(reason = "Called from uninterruptible code.", mayBeInlined = true)
    public static long getTraceId(Class<?> clazz) {
        long traceid = getTraceIdRaw(clazz);
        return traceid >>> TRACE_ID_SHIFT;
    }

    @Uninterruptible(reason = "Called from uninterruptible code.", mayBeInlined = true)
    public static long load(Class<?> clazz) {
        return JfrTraceIdLoadBarrier.load(clazz);
    }

    @Platforms(Platform.HOSTED_ONLY.class)
    private static void tagAsJdkJfrEvent(int index) {
        JfrTraceIdMap map = getTraceIdMap();
        long id = map.getId(index);
        map.setId(index, id | JDK_JFR_EVENT_CLASS);
    }

    @Platforms(Platform.HOSTED_ONLY.class)
    private static void tagAsJdkJfrEventSub(int index) {
        JfrTraceIdMap map = getTraceIdMap();
        long id = map.getId(index);
        map.setId(index, id | JDK_JFR_EVENT_SUBCLASS);
    }

    private static boolean isEventClass(int index) {
        JfrTraceIdMap map = getTraceIdMap();
        long id = map.getId(index);
        return (id & (JDK_JFR_EVENT_CLASS | JDK_JFR_EVENT_SUBCLASS)) != 0;
    }

    @Platforms(Platform.HOSTED_ONLY.class)
    public static void assign(Class<?> clazz, int index) {
        assert clazz != null;
        if (getTraceIdMap().getId(index) != -1) {
            return;
        }
        long typeId = getTypeId(clazz);
        getTraceIdMap().setId(index, typeId << TRACE_ID_SHIFT);

        if ((jdk.internal.event.Event.class == clazz || jdk.jfr.Event.class == clazz) &&
                        clazz.getClassLoader() == null || clazz.getClassLoader() == ClassLoader.getSystemClassLoader()) {
            tagAsJdkJfrEvent(index);
        }
        if ((jdk.internal.event.Event.class.isAssignableFrom(clazz) || jdk.jfr.Event.class.isAssignableFrom(clazz)) &&
                        clazz.getClassLoader() == null || clazz.getClassLoader() == ClassLoader.getSystemClassLoader()) {
            tagAsJdkJfrEventSub(index);
        }
    }

    private static long getTypeId(Class<?> clazz) {
        /*
         * We are picking up the host trace-ID here. This is important because host JFR will build
         * some datastructures that preserve the trace-IDs itself, and those end up in the image. We
         * need to be in sync with that information, otherwise events may get dropped or other
         * inconsistencies.
         */
        if (clazz == void.class) {
            /*
             * void doesn't seem to be one of the known types in Type.java, but it would crash when
             * queried in Hotspot. Trouble is that some code appears to be calling JVM.getTypeId()
             * or similar at run-time, at which point it's expected that we have an entry in the
             * table. Let's map it to 0 which is also TYPE_NONE in Hotspot's JFR. Maybe it should be
             * added to the known types in Hotspot's Type.java.
             */
            return 0;
        } else {
            return Type.getTypeId(clazz);
        }
    }
}
