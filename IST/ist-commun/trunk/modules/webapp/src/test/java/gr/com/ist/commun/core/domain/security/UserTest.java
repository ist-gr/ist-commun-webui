package gr.com.ist.commun.core.domain.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import gr.com.ist.commun.core.domain.TimePeriod;
import gr.com.ist.commun.core.domain.security.User;
import gr.com.ist.commun.core.service.TimeService;

import java.util.Date;

import org.junit.Test;

public class UserTest {
    private TimeService frozenTime = new TimeService() {
        @Override
        public long currentTimeMillis() {
            return 0;
        }
    };

    // TODO: Try http://www.scalacheck.org/ to implement the following test
    // cases
    @Test
    public void testGivenNullValidityPeriodThenAccountIsNotExpired() {
        User tested = new User();
        tested.setTimeService(frozenTime);
        assertNull(tested.getValidFor());
        assertTrue(tested.isAccountNonExpired());
    }

    @Test
    public void testGivenNullValidityPeriodStartDateAndEndDateThenAccountIsNotExpired() {
        User tested = new User();
        tested.setTimeService(frozenTime);
        tested.setValidFor(new TimePeriod());
        assertNull(tested.getValidFor().getStartDateTime());
        assertNull(tested.getValidFor().getEndDateTime());
        assertTrue(tested.isAccountNonExpired());
    }

    @Test
    public void testGivenNullValidityPeriodStartDateAndFutureEndDateThenAccountIsNotExpired() {
        User tested = new User();
        tested.setTimeService(frozenTime);
        tested.setValidFor(new TimePeriod());
        tested.getValidFor().setEndDateTime(new Date(frozenTime.currentTimeMillis() + 1));
        assertNull(tested.getValidFor().getStartDateTime());
        assertTrue(tested.isAccountNonExpired());
    }

    @Test
    public void testGivenNullValidityPeriodStartDateAndPresentEndDateThenAccountIsNotExpired() {
        User tested = new User();
        tested.setTimeService(frozenTime);
        tested.setValidFor(new TimePeriod());
        tested.getValidFor().setEndDateTime(new Date(frozenTime.currentTimeMillis()));
        assertNull(tested.getValidFor().getStartDateTime());
        assertTrue(tested.isAccountNonExpired());
    }

    @Test
    public void testGivenNullValidityPeriodStartDateAndPastEndDateThenAccountIsExpired() {
        User tested = new User();
        tested.setTimeService(frozenTime);
        tested.setValidFor(new TimePeriod());
        tested.getValidFor().setEndDateTime(new Date(frozenTime.currentTimeMillis() - 1));
        assertNull(tested.getValidFor().getStartDateTime());
        assertFalse(tested.isAccountNonExpired());
    }

    @Test
    public void testGivenNullValidityPeriodEndDateAndPastStartDateTheAccounIsNotExpired() {
        User tested = new User();
        tested.setTimeService(frozenTime);
        tested.setValidFor(new TimePeriod());
        tested.getValidFor().setStartDateTime(new Date(frozenTime.currentTimeMillis() - 1));
        assertNull(tested.getValidFor().getEndDateTime());
        assertTrue(tested.isAccountNonExpired());
    }

    @Test
    public void testGivenNullValidityPeriodEndDateAndPresentStartDateTheAccounIsNotExpired() {
        User tested = new User();
        tested.setTimeService(frozenTime);
        tested.setValidFor(new TimePeriod());
        tested.getValidFor().setStartDateTime(new Date(frozenTime.currentTimeMillis()));
        assertNull(tested.getValidFor().getEndDateTime());
        assertTrue(tested.isAccountNonExpired());
    }

    @Test
    public void testGivenNullValidityPeriodEndDateAndFutureStartDateTheAccounIsExpired() {
        User tested = new User();
        tested.setTimeService(frozenTime);
        tested.setValidFor(new TimePeriod());
        tested.getValidFor().setStartDateTime(new Date(frozenTime.currentTimeMillis() + 1));
        assertNull(tested.getValidFor().getEndDateTime());
        assertFalse(tested.isAccountNonExpired());
    }

    @Test
    public void testGivenValidityPeriodWhenCurrentTimeIsWithinPeriodTheAccounIsNotExpired() {
        User tested = new User();
        tested.setTimeService(frozenTime);
        tested.setValidFor(new TimePeriod());
        tested.getValidFor().setStartDateTime(new Date(frozenTime.currentTimeMillis() - 1));
        tested.getValidFor().setEndDateTime(new Date(frozenTime.currentTimeMillis() + 1));
        assertTrue(tested.isAccountNonExpired());
    }

    @Test
    public void testGivenValidityPeriodWhenCurrentTimeIsOutsideOfPeriodTheAccounExpired() {
        User tested = new User();
        tested.setTimeService(frozenTime);
        tested.setValidFor(new TimePeriod());
        tested.getValidFor().setStartDateTime(new Date(frozenTime.currentTimeMillis() + 1));
        tested.getValidFor().setEndDateTime(new Date(frozenTime.currentTimeMillis() + 2));
        assertFalse(tested.isAccountNonExpired());
    }

    @Test
    public void testWhenLockedIsNullThenAccountIsNotLocked() {
        User tested = new User();
        assertNull(tested.isLocked());
        assertTrue(tested.isAccountNonLocked());
    }

    @Test
    public void testWhenDisabledIsNullThenAccountIsEnabled() {
        User tested = new User();
        assertNull(tested.isDisabled());
        assertTrue(tested.isEnabled());
    }

    @Test
    public void testWhenPasswordExpiresOnIsNullThenCredentialsIsNonExpired() {
        User tested = new User();
        assertNull(tested.getPasswordExpiresOn());
        assertTrue(tested.isCredentialsNonExpired());
    }
    
    @Test
    public void testThatPasswordIsNotReencoded() {
        String plainText = "123";
        User tested = new User();
        tested.setPassword(plainText);
        assertEquals(plainText, tested.getPassword());
        tested.encryptPasswordField();
        assertFalse(plainText.equals(tested.getPassword()));
        String encodedPassword = tested.getPassword();
        tested.setPassword(encodedPassword);
        assertEquals(encodedPassword, tested.getPassword());
        tested.encryptPasswordField();
        assertEquals(encodedPassword, tested.getPassword());
    }

}
