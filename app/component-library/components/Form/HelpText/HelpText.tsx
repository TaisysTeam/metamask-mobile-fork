/* eslint-disable react/prop-types */

// Third party dependencies.
import React from 'react';

// External dependencies.
import Text, { TextVariant } from '../../Texts/Text';

// Internal dependencies.
import { HelpTextProps } from './HelpText.types';
import {
  DEFAULT_HELPTEXT_SEVERITY,
  TEXT_COLOR_BY_HELPTEXT_SEVERITY,
} from './HelpText.constants';

const HelpText: React.FC<HelpTextProps> = ({
  severity = DEFAULT_HELPTEXT_SEVERITY,
  ...props
}) => (
  <Text
    variant={TextVariant.BodySM}
    color={TEXT_COLOR_BY_HELPTEXT_SEVERITY[severity]}
    {...props}
  />
);

export default HelpText;
