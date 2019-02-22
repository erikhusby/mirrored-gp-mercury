package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A set of static methods that return {@link MolecularIndexingScheme}s that contain
 * the correct {@link MolecularIndex}es in the correct positions. These schemes may
 * have been retrieved from the database or created de novo. New schemes are not
 * persisted.
 */
@Dependent
public class MolecularIndexingSchemeFactory {

    @Inject
    private MolecularIndexingSchemeDao schemeDao;

    @Inject
    private MolecularIndexDao indexDao;

    private Map<List<IndexPositionPair>, MolecularIndexingScheme> cachedSchemes =
            new HashMap<>();

    public static class IndexPositionPair {
        private final String sequence;
        private final MolecularIndexingScheme.IndexPosition position;

        public IndexPositionPair(MolecularIndexingScheme.IndexPosition position, String sequence) {
            this.sequence = sequence;
            this.position = position;
        }

        public String getSequence() {
            return this.sequence;
        }

        public MolecularIndexingScheme.IndexPosition getPositionHint() {
            return this.position;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof IndexPositionPair)) {
                return false;
            }

            IndexPositionPair that = (IndexPositionPair) o;

            if (!position.equals(that.getPositionHint())) {
                return false;
            }
            if (!sequence.equals(that.getSequence())) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = sequence.hashCode();
            result = 31 * result + position.hashCode();
            return result;
        }
    }

    static List<IndexPositionPair> getPairArray(MolecularIndexingScheme scheme, IndexPositionPair... indexPositionPairs) {
        List<IndexPositionPair> pairList = new ArrayList<>(); //new IndexPositionPair[scheme.getIndexes().size() + indexPositionPairs.length];

        for (Map.Entry<MolecularIndexingScheme.IndexPosition, MolecularIndex> entry : scheme.getIndexes().entrySet()) {
            pairList.add(new IndexPositionPair(entry.getKey(), entry.getValue().getSequence()));
        }

        for (IndexPositionPair pair : indexPositionPairs) {
            pairList.add(pair);
        }

        return pairList;
    }

    /**
     * Finds or creates a {@link MolecularIndexingScheme} that contains the provided
     * {@link MolecularIndex} at the given position. No parameters may be null. Throws
     * {@link IllegalArgumentException} if no molecular index with the given name is
     * found.
     */
    public MolecularIndexingScheme findOrCreateIndexingScheme(
            final MolecularIndexingScheme.IndexPosition position,
            final String indexSequence)
    {
        if (position == null) {
            throw new NullPointerException("Position must not be null.");
        }
        if (indexSequence == null) {
            throw new NullPointerException("The index sequence must not be null.");
        }

        return this.findOrCreateIndexingScheme(new ArrayList<IndexPositionPair>(){{add(new IndexPositionPair(position, indexSequence));}},
                false);
    }

    /**
     * Finds or creates a {@link MolecularIndexingScheme} that has the indexes (with
     * their positions) of the given base scheme, plus the new index at the given new
     * position. No parameters may be null. Throws {@link IllegalArgumentException}
     * if no molecular index with the given name is found.
     */
    public MolecularIndexingScheme findOrCreateIndexingScheme(
            MolecularIndexingScheme baseScheme,
            MolecularIndexingScheme.IndexPosition position,
            String indexSequence)
    {
        if (baseScheme == null) {
            throw new NullPointerException("The base scheme cannot be null.");
        }
        if (position == null) {
            throw new NullPointerException("Position must not be null.");
        }
        if (indexSequence == null) {
            throw new NullPointerException("The index sequence must not be null.");
        }

        return this.findOrCreateIndexingScheme(
                getPairArray(baseScheme, new IndexPositionPair(position, indexSequence)), false);
    }

    /**
     * Finds a {@link MolecularIndexingScheme} in the database with the given index/
     * position pairs, if one exists, or creates a new scheme if one does not.
     *
     * @param indexPositionPairs The array of position/index pairs. Must not be empty or null.
     * @param createIndexes If true, creates a new index from an unknown sequence. If false, throws
     * IllegalArgumentException if any of the sequences in the pair array don't have a
     * corresponding MolecularIndex in the database.
     */
    public MolecularIndexingScheme findOrCreateIndexingScheme(List<IndexPositionPair> indexPositionPairs,
            boolean createIndexes) {
        MolecularIndexingScheme cachedScheme = cachedSchemes.get(indexPositionPairs);
        if(cachedScheme != null) {
            return cachedScheme;
        }
        MolecularIndexingScheme foundScheme = findIndexingScheme(indexPositionPairs);
        if (foundScheme != null) {
            cachedSchemes.put(indexPositionPairs, foundScheme);
            return foundScheme;
        }

        Map<MolecularIndexingScheme.IndexPosition, MolecularIndex> positionIndexMap = new HashMap<>();
        for (IndexPositionPair pair : indexPositionPairs) {
            MolecularIndex index = this.indexDao.findBySequence(pair.getSequence());
            if (index == null) {
                if (createIndexes) {
                    index = new MolecularIndex(pair.getSequence());
                    indexDao.persist(index);
                } else {
                    throw new IllegalArgumentException(
                            "The sequence " + pair.getSequence() + " does not correspond to a known component index.");
                }
            }
            positionIndexMap.put(pair.getPositionHint(), index);
        }

        MolecularIndexingScheme scheme = new MolecularIndexingScheme(positionIndexMap);
        schemeDao.persist(scheme);
        schemeDao.flush();
        cachedSchemes.put(indexPositionPairs, scheme);
        return scheme;
    }

    void setMolecularIndexDao(MolecularIndexDao dao) {
        this.indexDao = dao;
    }

    void setMolecularIndexingSchemeDao(MolecularIndexingSchemeDao dao) {
        this.schemeDao = dao;
    }

    public MolecularIndexingScheme findIndexingScheme(List<IndexPositionPair> indexPositionPairs) {
        if (indexPositionPairs == null) {
            throw new NullPointerException("The list of index/position pairs must not be null.");
        }

        if (indexPositionPairs.size() == 0) {
            throw new IllegalArgumentException("The array of index/position pairs must have one or more entries");
        }

        switch (indexPositionPairs.size()) {
            case 1:
                return this.schemeDao.findSingleIndexScheme(
                        indexPositionPairs.get(0).getPositionHint().getIndexPosition(), indexPositionPairs.get(0).getSequence());
            case 2:
                return this.schemeDao.findDualIndexScheme(
                        indexPositionPairs.get(0).getPositionHint().getIndexPosition(), indexPositionPairs.get(0).getSequence(),
                        indexPositionPairs.get(1).getPositionHint().getIndexPosition(), indexPositionPairs.get(1).getSequence());
            case 3:
                return this.schemeDao.findTripleIndexScheme(
                        indexPositionPairs.get(0).getPositionHint().getIndexPosition(), indexPositionPairs.get(0).getSequence(),
                        indexPositionPairs.get(1).getPositionHint().getIndexPosition(), indexPositionPairs.get(1).getSequence(),
                        indexPositionPairs.get(2).getPositionHint().getIndexPosition(), indexPositionPairs.get(2).getSequence());
            default:
                throw new IllegalArgumentException("For now, up to three component indexes may be used in a single MolecularIndexingScheme.");
        }
    }
}
