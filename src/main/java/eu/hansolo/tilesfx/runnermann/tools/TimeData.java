/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2016-2021 Gerrit Grunwald.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.hansolo.tilesfx.runnermann.tools;

import eu.hansolo.tilesfx.runnermann.chart.ChartData;

import java.time.Instant;


/**
 * Created by hansolo on 01.11.16.
 */
public class TimeData extends ChartData {

    // ******************** Constructors **************************************
    public TimeData(final double VALUE) {
        super(VALUE, Instant.now());
    }
    public TimeData(final double VALUE, final Instant TIMESTAMP) {
        super(VALUE, TIMESTAMP);
    }


    // ******************** Methods *******************************************
    @Override public void setValue(final double VALUE) {}

    @Override public String toString() {
        return new StringBuilder().append("{\n")
                                  .append("  \"timestamp\":").append(super.getTimestamp().toEpochMilli()).append(",\n")
                                  .append("  \"value\":").append(super.getValue()).append("\n")
                                  .append("}")
                                  .toString();
    }
}
