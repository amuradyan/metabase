const { H } = cy;
import { SAMPLE_DATABASE } from "e2e/support/cypress_sample_database";

const { ORDERS_ID, ORDERS, PRODUCTS_ID, PRODUCTS } = SAMPLE_DATABASE;

describe("scenarios > question > trendline", () => {
  function setup(questionDetails) {
    H.restore();
    cy.signInAsNormalUser();
    H.createQuestion(questionDetails, { visitQuestion: true });
  }

  it("displays trendline when there are multiple numeric outputs (for simple question) (metabase#12781)", () => {
    setup({
      name: "12781",
      query: {
        "source-table": ORDERS_ID,
        aggregation: [
          ["avg", ["field", ORDERS.SUBTOTAL, null]],
          ["sum", ["field", ORDERS.TOTAL, null]],
        ],
        breakout: [["field", ORDERS.CREATED_AT, { "temporal-unit": "year" }]],
      },
      display: "line",
    });

    // Change settings to trendline
    H.openVizSettingsSidebar();
    H.leftSidebar().within(() => {
      cy.findByText("Display").click();
      cy.findByText("Trend line").click();
    });

    // Check graph is still there
    cy.get("rect");

    // Remove sum of total
    H.leftSidebar().within(() => {
      cy.findByText("Data").click();
      // eslint-disable-next-line no-unsafe-element-filtering
      cy.icon("close").last().click({ force: true });
      cy.findByText("Done").click();
    });

    // Graph should still exist
    cy.findByPlaceholderText("Created At").should("not.exist");
    cy.get("rect");
  });

  it("should display trend line for stack-100% chart (metabase#25614)", () => {
    setup({
      name: "25614",
      query: {
        "source-table": PRODUCTS_ID,
        aggregation: [["count"], ["avg", ["field", PRODUCTS.PRICE, null]]],
        breakout: [["field", PRODUCTS.CREATED_AT, { "temporal-unit": "year" }]],
      },
      display: "bar",
    });
    H.openVizSettingsSidebar();
    // stack 100%, then enable trend line
    H.leftSidebar().within(() => {
      cy.findByText("Display").click();
      cy.findByText("Stack - 100%").click();
      cy.findByText("Trend line").click();
    });
    // ensure that two trend lines are present
    H.trendLine().should("have.length", 2);
  });
});
