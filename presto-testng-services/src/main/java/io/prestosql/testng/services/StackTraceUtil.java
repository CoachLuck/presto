/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prestosql.testng.services;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.time.Duration;

public class StackTraceUtil {

    /**
     * The header section of the thread
     * @param threadInfo the thread information
     * @return the string builder of the header starting at a new line.
     */
    private static StringBuilder getHeader(ThreadInfo threadInfo)
    {
        StringBuilder sb = new StringBuilder("\"" + threadInfo.getThreadName() + "\"");
        sb.append("\n\tID: " + threadInfo.getThreadId());
        sb.append("\n\tSTATE: " + threadInfo.getThreadState());
        sb.append("\n\tSUSPENDED: " + threadInfo.isSuspended());
        sb.append("\n\tIN NATIVE: " + threadInfo.isInNative());
        sb.append("\n\tIS DAEMON: " + threadInfo.isDaemon());

        if (threadInfo.getLockName() != null) {
            sb.append("\n\n\tLOCK: ");
            sb.append(threadInfo.getLockName());
        }

        if (threadInfo.getLockOwnerName() != null) {
            sb.append("\n\t\tOWNER: " + threadInfo.getLockOwnerName());
            sb.append("\n\t\tOWNER ID: " + threadInfo.getLockOwnerId());
        }

        sb.append('\n');

        return sb;
    }

    /**
     * Appends the Thread State information into the log string.
     * @param sb the string builder of the log string
     * @param threadInfo the thread information
     * @param stackTracePos the stackTrace position
     * @return string builder with ThreadState information on Blocks/Waits
     */
    private static StringBuilder appendThreadStateInfo(StringBuilder sb, ThreadInfo threadInfo, int stackTracePos)
    {
        if(stackTracePos != 0 || threadInfo == null) return sb;
        Thread.State ts = threadInfo.getThreadState();
        switch (ts) {
            case BLOCKED:
                sb.append("\t-  BLOCKED on " + threadInfo.getLockInfo());
                sb.append("\n\t- BLOCKED for " + Duration.ofNanos(threadInfo.getBlockedTime()).toSeconds() + "s");
                break;
            case WAITING:
            case TIMED_WAITING:
                sb.append("\t-  WAITING on " + threadInfo.getLockInfo());
                sb.append("\n\t- WAITED for " + Duration.ofNanos(threadInfo.getWaitedTime()).toSeconds() + "s");
                break;
            default:
        }

        return sb;
    }

    /**
     * Appends the Locked Synchronizers, if any, to the log string
     * @param sb the string builder of the log string
     * @param locks the lock information from the thread info
     * @return the log message with appended locked synchronizer list, (class name/hashcode)
     */
    private static StringBuilder appendLockedSynchronizers(StringBuilder sb, LockInfo[] locks)
    {
        if(locks.length <= 0)
            return sb.append('\n');

        sb.append("\n\tNumber of LOCKED Synchronizers: " + locks.length);
        for (LockInfo li : locks) {
            sb.append("\n\t\tClass Name: " + li.getClassName());
            sb.append("\n\t\tHash Code: " + li.getIdentityHashCode());
            sb.append('\n');
        }

        return sb.append('\n');
    }

    /**
     * Appends the locked monitor list to the log message
     * @param sb the string builder of the log string
     * @param miList the list of locked monitors
     * @param stackTracePos the stack trace position
     * @return the log message with appended locked monitor list
     */
    private static StringBuilder appendLockedMonitors(StringBuilder sb, MonitorInfo[] miList, int stackTracePos) {
        if(miList.length <= 0) return sb;

        sb.append("\n\tLOCKED Monitors\n\t");
        for (MonitorInfo mi : miList) {
            // skip if we are not on the locked thread
            if(stackTracePos != mi.getLockedStackDepth())
                continue;

            sb.append("\t- " + mi);
            sb.append('\n');
        }

        return sb;
    }

    /**
     * Returns the full stacktrace string of the thread
     * @param threadInfo the thread information
     * @return the full stacktrace of the thread information
     */
    public static String getFullStackTrace(ThreadInfo threadInfo)
    {
        StringBuilder sb = getHeader(threadInfo);

        sb.append("\nStack Traces:\n");
        StackTraceElement[] stackTrace = threadInfo.getStackTrace();
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement ste = stackTrace[i];
            sb.append("\tat " + ste.toString());
            sb.append('\n');

            appendThreadStateInfo(sb, threadInfo, i);
            appendLockedMonitors(sb, threadInfo.getLockedMonitors(), i);
        }

        appendLockedSynchronizers(sb, threadInfo.getLockedSynchronizers());
        return sb.toString();
    }

}
