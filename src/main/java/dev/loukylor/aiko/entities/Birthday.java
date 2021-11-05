package dev.loukylor.aiko.entities;

import java.time.LocalDate;
import java.util.HashMap;

public class Birthday {
    public Long user = 0L;
    public LocalDate date = LocalDate.MIN;
    public LocalDate whenCelebrate = LocalDate.MIN;
    public HashMap<Long, String> customMessages = new HashMap<>();
}
