/*
    Copyright (C) 2019 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

    This file is part of Bus When? (Twin Cities).

    Bus When? (Twin Cities) is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    Bus When? (Twin Cities) is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Bus When? (Twin Cities); if not, see <http://www.gnu.org/licenses/>.
*/

package com.sweetiepiggy.buswhentwincities

import java.lang.Math.abs

sealed class NexTripChange {

    data class ItemInserted(val pos: Int): NexTripChange()
    data class ItemMoved(val fromPos: Int, val toPos: Int): NexTripChange()
    data class ItemRangeInserted(val posStart: Int, val itemCount: Int): NexTripChange()
    data class ItemRangeRemoved(val posStart: Int, val itemCount: Int): NexTripChange()
    data class ItemRemoved(val pos: Int): NexTripChange()
    data class ItemChanged(val pos: Int): NexTripChange()
    data class ItemRangeChanged(val posStart: Int, val itemCount: Int): NexTripChange()

    companion object {
        fun getNexTripChanges(origNexTrips: List<PresentableNexTrip>,
                newNexTrips: List<PresentableNexTrip>): List<NexTripChange> {
            // val structuralChanges: MutableList<NexTripChange> = mutableListOf()
            // val itemChanges: MutableList<NexTripChange> = mutableListOf()

            android.util.Log.d("abc", "got here orig: $origNexTrips")
            android.util.Log.d("abc", "got here new: $newNexTrips")

            // val origItr = origNexTrips.listIterator().withIndex()
            // val newItr = newNexTrips.listIterator().withIndex()

            // var done = !(origItr.hasNext() && newItr.hasNext())

            // while (!done) {
            //     var (origIdx, origNexTrip) = origItr.next()
            //     val (newIdx, newNexTrip) = newItr.next()

            //     if (!isSameNexTrip(origNexTrip, newNexTrip)) {
            //         dumpOrig.add(origNexTrip)
            //         if (origItr.hasNext()) {
            //             (origIdx, origNexTrip) = origItr.next()
            //         }
            //     } else if (!nexTripsAppearSame(origNexTrip, newNexTrip)) {
            //         itemChanges.add(NexTripChange.ItemChanged(newIdx))
            //         if (origItr.hasNext()) {
            //             (origIdx, origNexTrip) = origItr.next()
            //         }
            //         if (newItr.hasNext()) {
            //             (newIdx, newNexTrip) = newItr.next()
            //         }
            //     }
            // }

            val removes: MutableList<NexTripChange.ItemRemoved> = mutableListOf()
            val movesAndInserts: MutableList<Either<NexTripChange.ItemMoved, NexTripChange.ItemInserted>> =
                mutableListOf()
            val changes: MutableList<NexTripChange.ItemChanged> = mutableListOf()

            val newItr = newNexTrips.listIterator().withIndex()
            var newIdx: Int = -1
            var newNexTrip: PresentableNexTrip? = null
            if (newItr.hasNext()) {
                val (fst, snd) = newItr.next()
                newIdx = fst
                newNexTrip = snd
            }

            var origItr = origNexTrips.listIterator().withIndex()
            var removeCnt = 0
            var insertedCnt = 0

            while (origItr.hasNext()) {
                // come back to these later
                val dumpOrig: MutableList<IndexedValue<PresentableNexTrip>> = mutableListOf()

                for ((origIdx, origNexTrip) in origItr) {
                    if (newNexTrip == null) {
                        // origNexTrip doesn't exist in the new list
                        removes.add(NexTripChange.ItemRemoved(origIdx - removeCnt))
                        removeCnt += 1
                    } else if (!isSameNexTrip(origNexTrip, newNexTrip)) {
                        // this is not the NexTrip we are looking for, come back to it later
                        dumpOrig.add(IndexedValue(origIdx, origNexTrip))
                    } else {
                        if (origIdx + insertedCnt - dumpOrig.size != newIdx) {
                            movesAndInserts.add(Either.Left(NexTripChange.ItemMoved(origIdx + insertedCnt - dumpOrig.size, newIdx)))
                        }
                        // origNexTrip is still in list, make note if it changed
                        if (!nexTripsAppearSame(origNexTrip, newNexTrip)) {
                            changes.add(NexTripChange.ItemChanged(newIdx))
                        }
                        if (newItr.hasNext()) {
                            val (fst, snd) = newItr.next()
                            newIdx = fst
                            newNexTrip = snd
                        } else {
                            newIdx = -1
                            newNexTrip = null
                        }
                    }
                }

                if (newNexTrip != null) {
                    movesAndInserts.add(Either.Right(NexTripChange.ItemInserted(newIdx)))
                    insertedCnt += 1
                    if (newItr.hasNext()) {
                        val (fst, snd) = newItr.next()
                        newIdx = fst
                        newNexTrip = snd
                    } else {
                        newIdx = -1
                        newNexTrip = null
                    }
                }

                origItr = dumpOrig.listIterator()
            }

            if (newNexTrip != null) {
                movesAndInserts.add(Either.Right(NexTripChange.ItemInserted(newIdx)))
            }

            while (newItr.hasNext()) {
                movesAndInserts.add(Either.Right(NexTripChange.ItemInserted(newItr.next().index)))
            }

//            // find nexTrips that have been removed
//            for ((origIdx, origNexTrip) in origNexTrips.listIterator().withIndex()) {
//                if (!containsByBlockNumber(origNexTrip, newNexTrips)) {
//                    structuralChanges.add(NexTripChange.ItemRemoved(origIdx))
//                }
//            }
//
//            // find nexTrips that have been added
//            for ((newIdx, newNexTrip) in newNexTrips.listIterator().withIndex()) {
//                val (origIdx, origNexTrip) = indexOfByBlockNumber(newNexTrip, origNexTrips)
//                if (origIdx < 0) {
//                    structuralChanges.add(NexTripChange.ItemInserted(newIdx))
//                } else {
//                    if (origIdx != newIdx) {
//                        structuralChanges.add(NexTripChange.ItemMoved(origIdx, newIdx))
//                    }
//                    if (!nexTripsAppearSame(newNexTrip, origNexTrip!!)) {
//                        itemChanges.add(NexTripChange.ItemChanged(newIdx))
//                    }
//                }
//            }

            return groupRemoves(removes) + groupMovesAndInserts(movesAndInserts) + groupChanges(changes)
        }

        private fun groupRemoves(removes: List<NexTripChange.ItemRemoved>): List<NexTripChange> {
            if (removes.isEmpty()) {
                return removes
            }

            val groupedRemoves: MutableList<NexTripChange> = mutableListOf()
            val itr = removes.listIterator()
            var posStart = itr.next().pos
            var itemCount = 1

            while (itr.hasNext()) {
                val remove = itr.next()
                if (remove.pos == posStart) {
                    itemCount += 1
                } else if (remove.pos == posStart - 1) {
                    itemCount += 1
                    posStart = remove.pos
                } else {
                    groupedRemoves.add(if (itemCount == 1)
                        NexTripChange.ItemRemoved(posStart)
                    else
                		NexTripChange.ItemRangeRemoved(posStart, itemCount))
                    posStart = remove.pos
                    itemCount = 1
                }
            }

            groupedRemoves.add(if (itemCount == 1)
            	NexTripChange.ItemRemoved(posStart)
            else
            	NexTripChange.ItemRangeRemoved(posStart, itemCount))

            return groupedRemoves
        }


        private fun groupMovesAndInserts(movesAndInserts: List<Either<NexTripChange.ItemMoved, NexTripChange.ItemInserted>>): List<NexTripChange> {
            if (movesAndInserts.isEmpty()) {
                return listOf()
            }

            val groupedMovesAndInserts: MutableList<NexTripChange> = mutableListOf()
            val itr = movesAndInserts.listIterator()
            var posStart = -1
            var itemCount = 0

            while (itr.hasNext()) {
                val moveOrInsert = itr.next()
                when (moveOrInsert) {
                    is Either.Left -> {
                        val move = moveOrInsert.left
                        if (itemCount == 1) {
                            groupedMovesAndInserts.add(NexTripChange.ItemInserted(posStart))
                        } else if (itemCount > 1) {
                            groupedMovesAndInserts.add(NexTripChange.ItemRangeInserted(posStart, itemCount))
                        }
                        posStart = -1
                        itemCount = 0
                        groupedMovesAndInserts.add(move)
                    }
                    is Either.Right ->  {
                        val insert = moveOrInsert.right
                        if (insert.pos == posStart + itemCount) {
                            itemCount += 1
                        } else {
                            if (itemCount == 1) {
                                groupedMovesAndInserts.add(NexTripChange.ItemInserted(posStart))
                            } else if (itemCount > 1) {
                                groupedMovesAndInserts.add(NexTripChange.ItemRangeInserted(posStart, itemCount))
                            }
                            posStart = insert.pos
                            itemCount = 1
                        }
                    }
                }
            }

            if (itemCount == 1) {
                groupedMovesAndInserts.add(NexTripChange.ItemInserted(posStart))
            } else if (itemCount > 1) {
                groupedMovesAndInserts.add(NexTripChange.ItemRangeInserted(posStart, itemCount))
            }

            return groupedMovesAndInserts
        }

        private fun groupChanges(changes: List<NexTripChange.ItemChanged>): List<NexTripChange> {
            if (changes.isEmpty()) {
                return changes
            }

            val groupedChanges: MutableList<NexTripChange> = mutableListOf()
            val itr = changes.listIterator()
            var posStart = itr.next().pos
            var itemCount = 1

            while (itr.hasNext()) {
                val change = itr.next()
                if (change.pos == posStart + itemCount) {
                    itemCount += 1
                } else {
                    groupedChanges.add(if (itemCount == 1)
                        NexTripChange.ItemChanged(posStart)
                    else
                		NexTripChange.ItemRangeChanged(posStart, itemCount))
                    posStart = change.pos
                    itemCount = 1
                }
            }

            groupedChanges.add(if (itemCount == 1)
            	NexTripChange.ItemChanged(posStart)
            else
            	NexTripChange.ItemRangeChanged(posStart, itemCount))

            return groupedChanges
        }

        private fun isSameNexTrip(a: PresentableNexTrip, b: PresentableNexTrip) =
        	a.blockNumber == b.blockNumber && (a.departureTimeInMillis == b.departureTimeInMillis ||
                // turns out blockNumber is not unique, but hopefully it is unique per 10 minutes
        		abs(a.departureTimeInMillis!! - b.departureTimeInMillis!!) < 10 * 60 * 1000)

//        private fun containsByBlockNumber(nexTrip: PresentableNexTrip, nexTrips: List<PresentableNexTrip>): Boolean =
//            indexOfByBlockNumber(nexTrip, nexTrips).first >= 0
//
//        private fun indexOfByBlockNumber(nexTrip: PresentableNexTrip, nexTrips: List<PresentableNexTrip>): Pair<Int, PresentableNexTrip?> {
//            for ((idx, n) in nexTrips.listIterator().withIndex()) {
//                if (n.blockNumber == nexTrip.blockNumber && (n.departureTimeInMillis == nexTrip.departureTimeInMillis ||
//                		// turns out blockNumber is not unique, but hopefully it is unique per half hour
//                		abs(n.departureTimeInMillis!! - nexTrip.departureTimeInMillis!!) < 30 * 60 * 1000)) {
//                    return Pair(idx, n)
//                }
//            }
//            return Pair(-1, null)
//        }

        fun nexTripsAppearSame(nexTrip1: PresentableNexTrip, nexTrip2: PresentableNexTrip): Boolean =
            nexTrip1.departureText == nexTrip2.departureText &&
            nexTrip1.departureTime == nexTrip2.departureTime &&
            nexTrip1.isActual == nexTrip2.isActual &&
            nexTrip1.description == nexTrip2.description &&
            nexTrip1.routeAndTerminal == nexTrip2.routeAndTerminal &&
            nexTrip1.routeDirection == nexTrip2.routeDirection &&
    		(nexTrip1.position == null) == (nexTrip2.position == null)
        }
}

sealed class Either<A, B> {
    class Left<A, B>(val left: A): Either<A, B>()
    class Right<A, B>(val right: B): Either<A, B>()
}
