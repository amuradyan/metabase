import userEvent from "@testing-library/user-event";
import Color from "color";

import { render, screen } from "__support__/ui";
import { color, colors } from "metabase/lib/colors/palette";

import { ColorSettings } from "./ColorSettings";

describe("ColorSettings", () => {
  const initialColors = {
    brand: color("success"),
    accent1: color("text-medium"),
  };

  it("should update brand colors", async () => {
    const onChange = jest.fn();

    render(
      <ColorSettings
        initialColors={initialColors}
        originalColors={colors}
        onChange={onChange}
      />,
    );

    const input = screen.getByPlaceholderText(color("summarize"));
    await userEvent.clear(input);
    await userEvent.type(input, color("error"));

    expect(onChange).toHaveBeenLastCalledWith({
      brand: color("success"),
      /* Needs to convert this to hex because the input is transform to hex,
       * but we want to use hsla for our new colors, this is to allow better text search. */
      summarize: Color(color("error")).hex(),
      accent1: color("text-medium"),
    });
  });

  it("should update chart colors", async () => {
    const onChange = jest.fn();

    render(
      <ColorSettings
        initialColors={initialColors}
        originalColors={colors}
        onChange={onChange}
      />,
    );

    const input = screen.getByDisplayValue(color("text-medium"));
    await userEvent.clear(input);
    await userEvent.type(input, color("text-light"));

    expect(onChange).toHaveBeenLastCalledWith({
      brand: color("success"),
      accent1: color("text-light"),
    });
  });

  it("should reset chart colors", async () => {
    const onChange = jest.fn();

    render(
      <ColorSettings
        initialColors={initialColors}
        originalColors={colors}
        onChange={onChange}
      />,
    );

    await userEvent.click(screen.getByText("Reset to default colors"));
    await userEvent.click(screen.getByText("Reset"));

    expect(onChange).toHaveBeenLastCalledWith({
      brand: color("success"),
    });
  });

  it("should generate chart colors", async () => {
    const onChange = jest.fn();

    render(
      <ColorSettings
        initialColors={initialColors}
        originalColors={colors}
        onChange={onChange}
      />,
    );

    await userEvent.click(screen.getByText("Generate chart colors"));

    expect(onChange).toHaveBeenLastCalledWith({
      brand: color("success"),
      accent0: expect.any(String),
      accent1: color("text-medium"),
      accent2: expect.any(String),
      accent3: expect.any(String),
      accent4: expect.any(String),
      accent5: expect.any(String),
      accent6: expect.any(String),
      accent7: expect.any(String),
    });
  });
});
