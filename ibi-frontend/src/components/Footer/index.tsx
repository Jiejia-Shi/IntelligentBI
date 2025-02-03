import { GithubOutlined } from '@ant-design/icons';
import { DefaultFooter } from '@ant-design/pro-components';
import React from 'react';

const Footer: React.FC = () => {
  return (
    <DefaultFooter
      style={{
        background: 'none',
      }}
      links={[
        {
          key: 'Intelligent BI',
          title: 'Intelligent BI',
          href: 'https://github.com/Jiejia-Shi/IntelligentBI',
          blankTarget: true,
        },
        {
          key: 'github',
          title: <GithubOutlined />,
          href: 'https://github.com/Jiejia-Shi/IntelligentBI',
          blankTarget: true,
        },
        {
          key: 'Intelligent BI',
          title: 'Intelligent BI',
          href: 'https://github.com/Jiejia-Shi/IntelligentBI',
          blankTarget: true,
        },
      ]}
    />
  );
};

export default Footer;
