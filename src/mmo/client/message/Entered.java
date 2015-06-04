/*
 * Copyright 2015 Florian Hassanen
 *
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

package mmo.client.message;

import mmo.client.model.Coord;

public class Entered extends Coord implements Message {
    public Entered() {
        super(0, 0);
    }

    public Entered(int x, int y) {
        super(x, y);
    }

    @Override
    public String toString() {
        return "Entered{" + super.toString() + "}";
    }
}
