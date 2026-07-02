package com.danieljglover.allinslayer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Weakness
{
    private CombatStyle style;
    private String element;
}
