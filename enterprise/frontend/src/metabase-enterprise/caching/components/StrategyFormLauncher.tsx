import { t } from "ttag";
import { findWhere } from "underscore";

import { rootId } from "metabase/admin/performance/constants/simple";
import type { UpdateTargetId } from "metabase/admin/performance/types";
import { getShortStrategyLabel } from "metabase/admin/performance/utils";
import { FixedSizeIcon, Flex, Title, Tooltip, useHover } from "metabase/ui";
import type { CacheConfig } from "metabase-types/api";

import { PolicyToken, StyledLauncher } from "./StrategyFormLauncher.styled";

export type StrategyFormLauncherProps = {
  forId: number;
  targetId: number | null;
  title: string;
  updateTargetId: UpdateTargetId;
  configs: CacheConfig[];
  isFormDirty: boolean;
};

export const StrategyFormLauncher = ({
  forId,
  targetId,
  title,
  updateTargetId,
  configs,
  isFormDirty,
}: StrategyFormLauncherProps) => {
  const forRoot = forId === rootId;

  const config = findWhere(configs, { model_id: forId });
  const rootConfig = findWhere(configs, { model: "root" });

  const rootStrategy = rootConfig?.strategy ?? { type: "nocache" };
  const savedStrategy = config?.strategy;

  const inheritsRootStrategy = !forRoot && savedStrategy === undefined;
  const strategy = savedStrategy ?? rootStrategy;
  const isBeingEdited = targetId === forId;

  const { hovered, ref: hoveredRef } = useHover<HTMLDivElement>();

  const buttonVariant =
    isBeingEdited || hovered
      ? "filled"
      : inheritsRootStrategy || forRoot
        ? "default"
        : "outline";
  const shortStrategyLabel = getShortStrategyLabel(
    inheritsRootStrategy ? rootStrategy : strategy,
  );

  const ariaLabel = forRoot
    ? t`Edit default policy (currently: ${shortStrategyLabel})`
    : inheritsRootStrategy
      ? t`Edit policy for database '${title}' (currently inheriting the default policy, ${getShortStrategyLabel(
          rootStrategy,
        )})`
      : t`Edit policy for database '${title}' (currently: ${shortStrategyLabel})`;

  const launchForm = () => {
    if (targetId !== forId) {
      updateTargetId(forId, isFormDirty);
    }
  };
  const shouldDisableTooltip = !inheritsRootStrategy;

  return (
    <StyledLauncher
      ref={hoveredRef as any}
      aria-label={ariaLabel}
      onClick={launchForm}
      forRoot={forRoot}
      inheritsRootStrategy={inheritsRootStrategy}
      data-testid={`strategy-form-launcher${
        shouldDisableTooltip ? "" : "-with-tooltip"
      }`}
    >
      <Flex gap="0.5rem" color="text-medium" align="center">
        <FixedSizeIcon name={forRoot ? "star" : "database"} color="inherit" />
        <Title c="inherit" order={6}>
          {title}
        </Title>
      </Flex>
      <Flex wrap="nowrap" lh="1.5rem" gap=".5rem">
        <Tooltip
          position="bottom"
          disabled={shouldDisableTooltip}
          label={t`Using default policy`}
        >
          <PolicyToken
            onClick={launchForm}
            variant={buttonVariant}
            fw={forRoot || inheritsRootStrategy ? "normal" : "bold"}
            lh="1.5rem"
            p="0.25rem .75rem"
            mah="3rem"
            radius="7rem"
          >
            {shortStrategyLabel}
          </PolicyToken>
        </Tooltip>
      </Flex>
    </StyledLauncher>
  );
};
