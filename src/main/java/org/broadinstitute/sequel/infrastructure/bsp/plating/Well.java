package org.broadinstitute.sequel.infrastructure.bsp.plating;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Well {
    
    private char row;
    private int col;
    private Plateable.Size size;

    private static final Log logger = LogFactory.getLog(Well.class);

    /**
     * Wells can be constructed by index as long as the {@link Plateable.Order}
     * and {@link Plateable.Size} are specified.
     *
     * @param index
     * @param size
     * @param order
     */
    public Well(int index, Plateable.Size size, Plateable.Order order) {

        this.size = size;

        if (order == Plateable.Order.ROW) {
            row = (char) ('A' + index / size.getColumnCount());
            col = 1 + (index % size.getColumnCount());
        }
        else {
            row = (char) ('A' + index % size.getRowCount());
            col =  1 + index / size.getRowCount();

        }

        // logger.debug("built well: " + this);
    }


    /**
     * Construct by row and column with an overall {@link Plateable.Size} so we can later calculate indexes.
     *
     * @param row
     * @param col
     * @param size
     */
    public Well(char row, int col, Plateable.Size size) {
        this.size = size;
        this.row = row;
        this.col = col;
    }


    public char getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Plateable.Size getSize() {
        return size;
    }

    /**
     * zero-based index, useful for figuring out where to put empties in a plate
     *
     * @return
     */
    public int getIndex(Plateable.Order order) {

        int ret;
        
        if (order == Plateable.Order.ROW) {
            ret = (row - 'A') * size.getColumnCount();
            ret += col - 1;
        }
        else {
            ret = (col - 1) * size.getRowCount();
            ret += row - 'A';
        }

        return ret;

    }

    @Override
    public String toString() {
        return String.format("%c%02d", row, col);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Well)) return false;

        Well well = (Well) o;

        if (getIndex(Plateable.Order.ROW) != well.getIndex(Plateable.Order.ROW)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getIndex(Plateable.Order.ROW);
    }

}