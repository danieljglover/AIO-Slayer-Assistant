package com.danieljglover.allinslayer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonsterDefence
{
    private int defenceLevel;
    private int stab;
    private int slash;
    private int crush;
    private int magic;
    private int range;
}
