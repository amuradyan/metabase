// eslint-disable-next-line no-restricted-imports
import { css } from "@emotion/react";
// eslint-disable-next-line no-restricted-imports
import styled from "@emotion/styled";
import Draggable from "react-draggable";

import Button from "metabase/core/components/Button";
import { alpha, lighten } from "metabase/lib/colors";
import { Box } from "metabase/ui";

import TableS from "./TableInteractive.module.css";
import { getCellHoverBackground } from "./table-theme-utils";

export const TableInteractiveRoot = styled(Box)`
  .${TableS.TableInteractiveHeaderCellData} .${TableS.cellData} {
    border: 1px solid ${({ theme }) => alpha(theme.fn.themeColor("brand"), 0.2)};
  }

  .${TableS.TableInteractiveHeaderCellData} .${TableS.cellData}:hover {
    border: 1px solid
      ${({ theme }) => alpha(theme.fn.themeColor("brand"), 0.56)};
  }

  .${TableS.TableInteractiveCellWrapper}:hover {
    background-color: ${getCellHoverBackground};
  }
` as unknown as typeof Box;

interface TableDraggableProps {
  enableCustomUserSelectHack?: boolean;
}

// this wrapper adds prop-types warning because of the nature of emotion component
// TableDraggable doesn't allow className, style to be passed, but emotion does it
// TODO: drop this style definition and maybe move styles to the global stylesheet
export const TableDraggable = styled(Draggable)<TableDraggableProps>`
  ${props =>
    props.enableCustomUserSelectHack &&
    css`
      .react-draggable-transparent-selection *::-moz-selection {
        all: inherit;
      }

      .react-draggable-transparent-selection *::selection {
        all: inherit;
      }
    `}
`;

export const ResizeHandle = styled.div`
  &:active {
    background-color: var(--mb-color-brand);
  }

  &:hover {
    background-color: var(--mb-color-brand);
  }
`;

export const ExpandButton = styled(Button)`
  border: 1px solid
    ${({ theme }) => lighten(theme.fn?.themeColor("brand"), 0.3)};
  padding: 0.125rem 0.25rem;
  border-radius: 0.25rem;
  color: var(--mb-color-brand);
  margin-right: 0.5rem;
  margin-left: auto;

  &:hover {
    color: var(--mb-color-text-white);
    background-color: var(--mb-color-brand);
  }
`;
