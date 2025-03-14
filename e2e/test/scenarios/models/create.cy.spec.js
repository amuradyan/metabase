const { H } = cy;
import { THIRD_COLLECTION_ID } from "e2e/support/cypress_sample_instance_data";

const modelName = "A name";

describe("scenarios > models > create", () => {
  beforeEach(() => {
    H.restore();
    cy.signInAsAdmin();
    cy.intercept("POST", "/api/dataset").as("dataset");
  });

  it("creates a native query model via the New button", () => {
    cy.visit("/");

    navigateToNewModelPage();

    // Cancel creation with confirmation modal
    cy.findByTestId("dataset-edit-bar").button("Cancel").click();
    H.modal().button("Discard changes").click();

    // Now we will create a model
    navigateToNewModelPage();

    // Clicking on metadata should not work until we run a query
    cy.findByTestId("editor-tabs-metadata").should("be.disabled");

    H.NativeEditor.focus().type("select * from ORDERS");

    cy.findByTestId("native-query-editor-container").icon("play").click();
    cy.wait("@dataset");

    cy.findByTestId("dataset-edit-bar").button("Save").click();
    cy.findByPlaceholderText("What is the name of your model?").type(modelName);

    // eslint-disable-next-line no-unscoped-text-selectors -- deprecated usage
    cy.findByText("Save").click();

    // After saving, we land on view mode for the model
    // eslint-disable-next-line no-unscoped-text-selectors -- deprecated usage
    cy.findByText("Summarize");

    checkIfPinned();
  });

  it("suggest the currently viewed collection when saving a new native query", () => {
    H.visitCollection(THIRD_COLLECTION_ID);

    navigateToNewModelPage();
    H.NativeEditor.focus().type("select 1");
    cy.findByTestId("native-query-editor-container").icon("play").click();
    cy.wait("@dataset");

    cy.findByTestId("dataset-edit-bar").within(() => {
      cy.contains("button", "Save").click();
    });
    cy.findByTestId("save-question-modal").within(() => {
      cy.findByLabelText(/Where do you want to save this/).should(
        "have.text",
        "Third collection",
      );
    });
  });

  it("suggest the last accessed collection when saving a new structured query model", () => {
    H.visitCollection(THIRD_COLLECTION_ID);

    navigateToNewModelPage("structured");

    H.entityPickerModal().within(() => {
      H.entityPickerModalTab("Tables").click();
      cy.findByText("Orders").click();
    });

    cy.findByTestId("dataset-edit-bar").within(() => {
      cy.contains("button", "Save").click();
    });

    cy.findByTestId("save-question-modal").within(() => {
      cy.findByLabelText(/Where do you want to save this/).should(
        "have.text",
        "Third collection",
      );
    });
  });
});

function navigateToNewModelPage(queryType = "native") {
  H.newButton("Model").click();
  if (queryType === "structured") {
    cy.findByText("Use the notebook editor").click();
  } else {
    cy.findByText("Use a native query").click();
  }
}

function checkIfPinned() {
  H.visitCollection("root");

  cy.findByText(modelName)
    .closest("a")
    .find(".Icon-ellipsis")
    .click({ force: true });

  cy.findByText("Unpin").should("be.visible");
}
