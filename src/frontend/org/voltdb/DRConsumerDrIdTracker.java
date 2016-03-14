/* This file is part of VoltDB.
 * Copyright (C) 2008-2016 VoltDB Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.voltdb;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.voltdb.iv2.UniqueIdGenerator;

import com.google_voltpatches.common.collect.Maps;

public class DRConsumerDrIdTracker {

    private TreeMap<Long, Long> m_map = new TreeMap<Long, Long>();
    private long m_lastAckedDrId;
    private long m_lastSpUniqueId;
    private long m_lastMpUniqueId;

    public DRConsumerDrIdTracker(long initialAckPoint, long spUniqueId, long mpUniqueId) {
        m_lastAckedDrId = initialAckPoint;
        m_lastSpUniqueId = spUniqueId;
        m_lastMpUniqueId = mpUniqueId;
    }

    public DRConsumerDrIdTracker(DRConsumerDrIdTracker other) {
        m_map = new TreeMap<>(other.m_map);
        m_lastAckedDrId = other.m_lastAckedDrId;
        m_lastSpUniqueId = other.m_lastSpUniqueId;
        m_lastMpUniqueId = other.m_lastMpUniqueId;
    }

    public DRConsumerDrIdTracker(ByteBuffer buff) {
        m_lastAckedDrId = buff.getLong();
        m_lastSpUniqueId = buff.getLong();
        m_lastMpUniqueId = buff.getLong();
        int mapSize = buff.getInt();
        for (int ii=0; ii<mapSize; ii++) {
            m_map.put(buff.getLong(), buff.getLong());
        }
    }

    public DRConsumerDrIdTracker(byte[] flattened) {
        this(ByteBuffer.wrap(flattened));
    }

    public int getSerializedSize() {
        return 8        // m_lastAckedDrId
             + 8        // m_lastSpUniqueId
             + 8        // m_lastMpUniqueId
             + 4        // map size
             + (m_map.size() * 16);
    }

    public void serialize(ByteBuffer buff) {
        assert(buff.remaining() >= getSerializedSize());
        buff.putLong(m_lastAckedDrId);
        buff.putLong(m_lastSpUniqueId);
        buff.putLong(m_lastMpUniqueId);
        buff.putInt(m_map.size());
        for(Map.Entry<Long, Long> entry : m_map.entrySet()) {
            buff.putLong(entry.getKey());
            buff.putLong(entry.getValue());
        }
    }

    public void serialize(byte[] flattened) {
        ByteBuffer buff = ByteBuffer.wrap(flattened);
        serialize(buff);
    }

    public int size() {
        return m_map.size();
    }

    public void append(long startDrId, long endDrId, long spUniqueId, long mpUniqueId) {
        // There should never be keys past the append point
        assert(startDrId <= endDrId && startDrId > m_lastAckedDrId);
        assert(m_map.size() == 0 || m_map.lastEntry().getValue() < startDrId);

        // Note that any given range or tracker could have either sp or mp sentinel values
        m_lastSpUniqueId = Math.max(m_lastSpUniqueId, spUniqueId);
        m_lastMpUniqueId = Math.max(m_lastMpUniqueId, mpUniqueId);
        Map.Entry<Long, Long> prevEntry = m_map.lowerEntry(startDrId);
        if (prevEntry != null && prevEntry.getValue()+1 == startDrId) {
            // This entry can be merged with the previous one
            m_map.put(prevEntry.getKey(), endDrId);
        }
        else {
            m_map.put(startDrId, endDrId);
        }
    }

    public void appendTracker(DRConsumerDrIdTracker tracker) {
        assert(m_lastAckedDrId <= tracker.m_lastAckedDrId);

        // Note that any given range or tracker could have either sp or mp sentinel values
        m_lastSpUniqueId = Math.max(m_lastSpUniqueId, tracker.m_lastSpUniqueId);
        m_lastMpUniqueId = Math.max(m_lastMpUniqueId, tracker.m_lastMpUniqueId);

        assert (tracker.size() > 0);
        if (m_map.isEmpty()) {
            m_map = new TreeMap<>(tracker.m_map);
        }
        else {
            Map.Entry<Long, Long> lastEntry = m_map.lastEntry();
            for (Entry<Long, Long> newEntry : tracker.m_map.entrySet()) {
                // There should never be keys past the append point
                assert(lastEntry.getValue() < newEntry.getKey());
                if (lastEntry.getValue()+1 == newEntry.getKey()) {
                    // consolidate the two ranges
                    m_map.put(lastEntry.getKey(), newEntry.getValue());
                    lastEntry = Maps.immutableEntry(lastEntry.getKey(), newEntry.getValue());
                } else {
                    m_map.put(newEntry.getKey(), newEntry.getValue());
                    lastEntry = newEntry;
                }
            }
        }
        truncate(tracker.m_lastAckedDrId);
    }

    private void put(long startDrId, long endDrId) {
        Map.Entry<Long, Long> nextEntry = m_map.higherEntry(endDrId);
        Map.Entry<Long, Long> prevEntry = m_map.lowerEntry(startDrId);
        assert(!m_map.containsKey(startDrId) && !m_map.containsKey(endDrId));
        if (prevEntry != null && prevEntry.getValue()+1 == startDrId) {
            // This entry can be merged with the previous one
            if (nextEntry!= null && endDrId+1 == nextEntry.getKey()) {
                // The entry fills a hole between two
                m_map.remove(nextEntry.getKey());
                m_map.put(prevEntry.getKey(), nextEntry.getValue());
            }
            else {
                assert(nextEntry == null || endDrId < nextEntry.getKey());
                m_map.put(prevEntry.getKey(), endDrId);
            }
        }
        else {
            assert(prevEntry == null || prevEntry.getValue() < startDrId);
            if (nextEntry != null && endDrId+1 == nextEntry.getKey()) {
                // This value should replace one ahead of this one
                m_map.remove(nextEntry.getKey());
                m_map.put(startDrId, nextEntry.getValue());
            }
            else {
                assert(nextEntry == null || endDrId < nextEntry.getKey());
                m_map.put(startDrId, endDrId);
            }
        }
    }

    public void truncate(long newTruncationPoint) {
        assert(newTruncationPoint >= m_lastAckedDrId);
        Map.Entry<Long, Long> firstEntry = m_map.firstEntry();
        while (firstEntry != null && firstEntry.getKey() <= newTruncationPoint) {
            m_map.remove(firstEntry.getKey());
            if (firstEntry.getValue() > newTruncationPoint) {
                m_map.put(newTruncationPoint+1, firstEntry.getValue());
                break;
            }
            firstEntry = m_map.firstEntry();
        }
        m_lastAckedDrId = newTruncationPoint;
    }

    private void doMerge(DRConsumerDrIdTracker tracker) {
        for(Map.Entry<Long, Long> entry : tracker.m_map.entrySet()) {
            if (entry.getValue() <= m_lastAckedDrId) {
                // skip entries before the truncation (ack) point
                continue;
            }
            else if (entry.getKey() <= m_lastAckedDrId) {
                // entries that cross the truncation (ack) point need to be adjusted up
                put(m_lastAckedDrId+1, entry.getValue());
            }
            else {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void mergeTracker(DRConsumerDrIdTracker tracker) {
        if (tracker.m_lastAckedDrId > m_lastAckedDrId) {
            truncate(tracker.m_lastAckedDrId);
        }
        doMerge(tracker);
        m_lastSpUniqueId = Math.max(m_lastSpUniqueId, tracker.m_lastSpUniqueId);
        m_lastMpUniqueId = Math.max(m_lastMpUniqueId, tracker.m_lastMpUniqueId);
    }

    public long advancedAckPoint() {
        Entry<Long, Long> firstEntry = m_map.firstEntry();
        if (firstEntry != null && m_lastAckedDrId+1 == firstEntry.getKey()) {
            // Advance the ackpoint
            m_lastAckedDrId = firstEntry.getValue();
            m_map.remove(firstEntry.getKey());
        }
        return m_lastAckedDrId;
    }

    /**
     * Check if this tracker contains a given DR id
     * @param startDrId
     * @return the entry if this tracker has an entry contains the given DR id, otherwise returns null
     */
    public Map.Entry<Long, Long> contains(Long startDrId) {
        Map.Entry<Long, Long> prevEntry = m_map.floorEntry(startDrId);
        if (prevEntry != null && startDrId >= prevEntry.getKey() && startDrId <= prevEntry.getValue()) {
            return prevEntry;
        }
        return null;
    }

    public long getLastAckedDrId() {
        return m_lastAckedDrId;
    }

    public long getLastSpUniqueId() {
        return m_lastSpUniqueId;
    }

    public long getLastMpUniqueId() {
        return m_lastMpUniqueId;
    }

    public TreeMap<Long, Long> getDrIdRanges() {
        return m_map;
    }

    public String debugInfo() {
        if (m_map.isEmpty()) {
            return "Empty Map";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("lastAckPoint ").append(DRLogSegmentId.getClusterIdFromDRId(m_lastAckedDrId)).append(":")
          .append(DRLogSegmentId.getSentinelOrSeqNumFromDRId(m_lastAckedDrId)).append(" ");
        sb.append("lastSpUniqueId ").append(UniqueIdGenerator.toShortString(m_lastSpUniqueId)).append(" ");
        sb.append("lastMpUniqueId ").append(UniqueIdGenerator.toShortString(m_lastMpUniqueId)).append(" ");
        for (Map.Entry<Long, Long> entry : m_map.entrySet()) {
            sb.append("[").append(DRLogSegmentId.getSequenceNumberFromDRId(entry.getKey())).append(", ")
              .append(DRLogSegmentId.getSequenceNumberFromDRId(entry.getValue())).append("] ");
        }
        return sb.toString();
    }
}