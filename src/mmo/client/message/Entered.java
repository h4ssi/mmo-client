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
