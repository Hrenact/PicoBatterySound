package com.pico.batterysound;

public final class BatteryPolicyTest {
    private static int assertions;
    private static void expect(BatteryPolicy.Event actual, BatteryPolicy.Event expected) {
        assertions++;
        if (actual != expected) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
    public static void main(String[] args) {
        BatteryPolicy p = new BatteryPolicy();
        expect(p.update(80, false, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(80, true, 20, 5), BatteryPolicy.Event.CHARGING);
        expect(p.update(81, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(21, false, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(20, false, 20, 5), BatteryPolicy.Event.LOW);
        expect(p.update(20, false, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(21, false, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(19, false, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(4, false, 20, 5), BatteryPolicy.Event.CRITICAL);
        expect(p.update(3, false, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(3, true, 20, 5), BatteryPolicy.Event.CHARGING);
        expect(p.update(3, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(3, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(3, false, 20, 5), BatteryPolicy.Event.CRITICAL);
        p = new BatteryPolicy();
        expect(p.update(4, false, 20, 5), BatteryPolicy.Event.CRITICAL);
        expect(p.update(4, false, 20, 5), BatteryPolicy.Event.NONE);
        p = new BatteryPolicy();
        expect(p.update(4, true, 20, 5), BatteryPolicy.Event.CRITICAL);
        expect(p.update(-1, false, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(101, false, 20, 5), BatteryPolicy.Event.NONE);
        p = new BatteryPolicy();
        expect(p.update(19, false, 20, 5), BatteryPolicy.Event.LOW);
        p = new BatteryPolicy(p.snapshot());
        expect(p.update(18, false, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(5, false, 20, 5), BatteryPolicy.Event.CRITICAL);
        p = new BatteryPolicy(p.snapshot());
        expect(p.update(4, false, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(4, true, 20, 5), BatteryPolicy.Event.CHARGING);
        p = new BatteryPolicy(p.snapshot());
        expect(p.update(5, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(5, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(4, false, 20, 5), BatteryPolicy.Event.CRITICAL);
        // Wired streaming: power remains connected throughout the discharge.
        p = new BatteryPolicy();
        expect(p.update(32, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(16, true, 20, 5), BatteryPolicy.Event.LOW);
        expect(p.update(15, true, 20, 5), BatteryPolicy.Event.NONE);
        p = new BatteryPolicy(p.snapshot());
        expect(p.update(14, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(5, true, 20, 5), BatteryPolicy.Event.CRITICAL);
        expect(p.update(4, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(6, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(5, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(7, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(5, true, 20, 5), BatteryPolicy.Event.CRITICAL);
        expect(p.update(21, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(20, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(22, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(20, true, 20, 5), BatteryPolicy.Event.LOW);
        p = new BatteryPolicy();
        expect(p.update(32, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(3, true, 20, 5), BatteryPolicy.Event.CRITICAL);
        expect(p.update(3, true, 20, 5), BatteryPolicy.Event.NONE);
        // Plugging in below low must not echo low, but further descent to critical must warn.
        p = new BatteryPolicy();
        expect(p.update(16, false, 20, 5), BatteryPolicy.Event.LOW);
        expect(p.update(16, true, 20, 5), BatteryPolicy.Event.CHARGING);
        expect(p.update(16, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(15, true, 20, 5), BatteryPolicy.Event.NONE);
        expect(p.update(5, true, 20, 5), BatteryPolicy.Event.CRITICAL);
        expect(p.update(5, true, 20, 5), BatteryPolicy.Event.NONE);
        System.out.println("Passed " + assertions + " battery policy checks.");
    }
}
