package gr.com.ist.commun.seqgen;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
public class SequenceGeneratorService {
    private static final String SQL_SELECT = "SELECT LAST_ASSIGNED_VALUE FROM SEQUENCE_GENERATOR WHERE KEY = ?";
    private static final String SQL_UPDATE = "UPDATE SEQUENCE_GENERATOR set LAST_ASSIGNED_VALUE = (LAST_ASSIGNED_VALUE + ?) WHERE KEY = (?)";
    private static final String SQL_INSERT = "INSERT INTO SEQUENCE_GENERATOR (KEY, LAST_ASSIGNED_VALUE) VALUES (?,?)";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Allocates a range of continuous integers for the requested key and allocation size.
     * 
     * @param key The key of the sequence. The key can be 254 characters maximum.
     * @param allocationSize The number of the continuous integers to be allocated.
     * @return The first integer of the allocated range.
     */
    @Transactional
    public Integer getNext(String key, Integer allocationSize) {
        Assert.notNull(allocationSize);
        Assert.notNull(key);
        int rows = jdbcTemplate.update(SQL_UPDATE, new Object[] { allocationSize, key });
        if (rows == 0) {
            SequenceGenerator newSequence = new SequenceGenerator();
            newSequence.setKey(key);
            newSequence.setLastAssignedValue(1);
            try {
                jdbcTemplate.update(SQL_INSERT, new Object[] { key, allocationSize });
                return 1;
            } catch (DuplicateKeyException ex) {
                jdbcTemplate.update(SQL_UPDATE, new Object[] { allocationSize, key });
            }
        }
        Integer lastAssignedValue = jdbcTemplate.queryForObject(SQL_SELECT, new Object[] { key }, Integer.class);
        return lastAssignedValue - allocationSize + 1;
    }
}