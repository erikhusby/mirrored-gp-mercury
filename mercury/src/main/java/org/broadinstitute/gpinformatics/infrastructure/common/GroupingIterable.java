package org.broadinstitute.gpinformatics.infrastructure.common;

import java.util.List;
import java.util.NoSuchElementException;


/**
 * Iterator/Iterable to break up a potentially large List into smaller Lists.
 * Instances of this might be useful for dealing with Oracle "in" 
 * expressions that can't contain more than 1000 elements, breaking up large
 * sets of samples for plating, etc.
 *  
 * @author mcovarr
 *
 * @param <T> The type of the element in the input List and generated Lists.
 */
// TODO Splitterize DELETE
public class GroupingIterable<T>  implements IterableWithSize<List<T>> {
    
    private int maxGroupSize;
    
    private List<T> inputList;
    
    private int currentGroup = -1;
    

    
    public GroupingIterable(int maxGroupSize, List<T> inputList) {
        if (maxGroupSize < 1)
            throw new IllegalArgumentException("Maximum size of iterable group must be at least 1");
        if (inputList == null)
            throw new IllegalArgumentException("input List can not be null");
        
        this.maxGroupSize = maxGroupSize;
        this.inputList = inputList;
    }

    @Override
    public boolean hasNext() {
        return (currentGroup + 1) * maxGroupSize < inputList.size();
    }

    @Override
    public List<T> next() {
        
        if (!hasNext())
            throw new NoSuchElementException();
        
        currentGroup++;
        
        int fromIndex = currentGroup * maxGroupSize;
        int toIndex = Math.min(((currentGroup + 1) * maxGroupSize), inputList.size());
        
        return inputList.subList(fromIndex, toIndex);
                
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GroupingIterable<T> iterator() {
        return this;
    }

    //@Override
    public int size() {
        return (inputList.size() + maxGroupSize - 1) / maxGroupSize;
    }


}
