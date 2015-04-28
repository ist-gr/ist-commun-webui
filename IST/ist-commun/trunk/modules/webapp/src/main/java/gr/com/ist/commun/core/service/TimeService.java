package gr.com.ist.commun.core.service;

import org.springframework.stereotype.Service;


public interface TimeService {
    long currentTimeMillis();
    @Service
    public static class Default implements TimeService {

        @Override
        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }
    }    
}
