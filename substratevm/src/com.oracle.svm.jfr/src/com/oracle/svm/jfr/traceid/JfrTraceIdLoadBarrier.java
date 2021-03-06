/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Consumer;

import com.oracle.svm.core.annotate.Uninterruptible;
import com.oracle.svm.core.heap.Heap;
import com.oracle.svm.core.jdk.UninterruptibleUtils;

/**
 * The trace id management class. This is used to tag classes as in use for the current epoch and to
 * iterate over them e.g. when writing the constant pools to disk.
 */
public class JfrTraceIdLoadBarrier {
    private static final UninterruptibleUtils.AtomicInteger classCount0 = new UninterruptibleUtils.AtomicInteger(0);
    private static final UninterruptibleUtils.AtomicInteger classCount1 = new UninterruptibleUtils.AtomicInteger(0);

    @Uninterruptible(reason = "Epoch may not change")
    private static boolean isNotTagged(long value) {
        long thisEpochBit = JfrTraceIdEpoch.getInstance().thisEpochBit();
        return ((value & ((thisEpochBit << JfrTraceId.META_SHIFT) | thisEpochBit)) != thisEpochBit);
    }

    @Uninterruptible(reason = "Epoch may not change")
    private static boolean shouldTag(Class<?> obj) {
        assert obj != null;
        return isNotTagged(JfrTraceId.getTraceIdRaw(obj));
    }

    @Uninterruptible(reason = "Epoch may not change")
    public static void clear(boolean epoch) {
        clearClassCount(epoch);
    }

    @Uninterruptible(reason = "Called from uninterruptible code.", mayBeInlined = true)
    private static void clearClassCount(boolean epoch) {
        if (epoch) {
            classCount1.set(0);
        } else {
            classCount0.set(0);
        }
    }

    @Uninterruptible(reason = "Called from uninterruptible code.", mayBeInlined = true)
    private static void increaseClassCount(boolean epoch) {
        if (epoch) {
            classCount1.incrementAndGet();
        } else {
            classCount0.incrementAndGet();
        }
    }

    public static int classCount(boolean epoch) {
        return epoch ? classCount1.get() : classCount0.get();
    }

    @Uninterruptible(reason = "Epoch must not change while in this method.")
    public static long load(Class<?> clazz) {
        assert clazz != null;
        if (shouldTag(clazz)) {
            JfrTraceId.setUsedThisEpoch(clazz);
            increaseClassCount(JfrTraceIdEpoch.getInstance().currentEpoch());
        }
        assert JfrTraceId.isUsedThisEpoch(clazz);
        return JfrTraceId.getTraceId(clazz);
    }

    public static void doClasses(ClassConsumer kc, boolean epoch) {
        long predicate = JfrTraceId.TRANSIENT_BIT;
        predicate |= epoch ? JfrTraceIdEpoch.EPOCH_1_BIT : JfrTraceIdEpoch.EPOCH_0_BIT;
        int usedClassCount = 0;
        for (Class<?> clazz : Heap.getHeap().getClassList()) {
            if (JfrTraceId.predicate(clazz, predicate)) {
                kc.accept(clazz);
                usedClassCount++;
                JfrTraceId.clearUsedThisEpoch(clazz, epoch);
            }
        }
        assert usedClassCount == classCount(epoch);
    }

    // Using Consumer<Class<?>> directly drags in other implementations which are not
    // uninterruptible.
    public interface ClassConsumer extends Consumer<Class<?>> {
    }
}
