/* eslint-disable react/prop-types */
import React from 'react';

// External dependencies.
import AvatarBlockies from './variants/AvatarBlockies';
import AvatarImage from './variants/AvatarImage';
import AvatarInitial from './variants/AvatarInitial';
import AvatarJazzIcon from './variants/AvatarJazzIcon';

// Internal dependencies.
import { AvatarProps, AvatarVariants } from './Avatar.types';
import {
  AVATAR_AVATAR_BLOCKIES_TEST_ID,
  AVATAR_AVATAR_IMAGE_TEST_ID,
  AVATAR_AVATAR_INITIAL_TEST_ID,
  AVATAR_AVATAR_JAZZICON_TEST_ID,
} from './Avatar.constants';

const Avatar = (avatarProps: AvatarProps) => {
  switch (avatarProps.variant) {
    case AvatarVariants.Blockies:
      return (
        <AvatarBlockies
          testID={AVATAR_AVATAR_BLOCKIES_TEST_ID}
          {...avatarProps}
        />
      );
    case AvatarVariants.Image:
      return (
        <AvatarImage testID={AVATAR_AVATAR_IMAGE_TEST_ID} {...avatarProps} />
      );
    case AvatarVariants.Initial:
      return (
        <AvatarInitial
          testID={AVATAR_AVATAR_INITIAL_TEST_ID}
          {...avatarProps}
        />
      );
    case AvatarVariants.JazzIcon:
      return (
        <AvatarJazzIcon
          testID={AVATAR_AVATAR_JAZZICON_TEST_ID}
          {...avatarProps}
        />
      );
    default:
      throw new Error('Invalid Avatar Variant');
  }
};

export default Avatar;
