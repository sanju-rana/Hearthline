import { describe, expect, it } from "vitest";
import { elapsed, inr } from "./utils";

describe("display helpers", () => {
  it("formats rupees", () => {
    expect(inr(418.95)).toContain("418.95");
  });

  it("shows elapsed minutes", () => {
    const eightMinutesAgo = new Date(Date.now() - 8 * 60 * 1000).toISOString();
    expect(elapsed(eightMinutesAgo)).toBe("8 min");
  });
});
