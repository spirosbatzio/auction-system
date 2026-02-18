package com.mtn.agent.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@ApplicationScoped
public class ValuationGenerator {

  private final Random rand = new Random();


  public Map<String, Double> generate(String type, int slotCount, int targetSlot) {
    Map<String, Double> valuations = new HashMap<>();

    switch (type.toUpperCase()) {
      case "RICH":
        for (int i = 0; i <= slotCount; i++) {
          if (rand.nextBoolean()) {
            valuations.put("SLOT_" + i, 30.0 + rand.nextDouble() * 20.0);
          }
        }
        break;
      case "POOR":
        for (int i = 0; i <= slotCount; i++) {
          if (rand.nextBoolean()) {
            valuations.put("SLOT_" + i, 5.0 + rand.nextDouble() * 10.0);
          }
        }
        break;
      case "FOCUSED":
        int slotWanted;
        if (targetSlot > 0 && targetSlot <= slotCount) {
          slotWanted = targetSlot;
        } else {
          slotWanted = rand.nextInt(slotCount) + 1;
        }
        valuations.put("SLOT_" + slotWanted, 100.0);
        break;
      case "BUNDLE_PAIR":
        valuations.put("SLOT_1", 25.0);
        valuations.put("SLOT_2", 25.0);
        break;
      case "FLEXIBLE_PAIR":
        valuations.put("SLOT_1", 30.0);
        valuations.put("SLOT_2", 30.0);
        break;
      case "RANDOM":
        // Assign random values to multiple slots
        for (int i = 1; i <= slotCount; i++) {
          if (rand.nextDouble() > 0.5) {  // 50% chance per slot
            valuations.put("SLOT_" + i, 10.0 + rand.nextDouble() * 20.0);
          }
        }
        // Ensure at least one slot has a value
        if (valuations.isEmpty() && slotCount > 0) {
          int randomSlot = rand.nextInt(slotCount) + 1;
          valuations.put("SLOT_" + randomSlot, 10.0 + rand.nextDouble() * 20.0);
        }
        break;
      default:
        for (int i = 0; i <= slotCount; i++) {
          if (rand.nextDouble() > 0.6) {
            valuations.put("SLOT_" + i, 10.0 + rand.nextDouble() * 20.0);
          }
        }
    }

    return valuations;
  }
}
