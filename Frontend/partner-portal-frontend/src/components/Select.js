import React from 'react';
import { Text } from '../custom-components';

const Select = ({ label, name, value, onChange, children }) => {
  return (
    <div style={{ marginBottom: 12 }}>
      <Text appearance="body-xs" color="primary-grey-80">
        {label}
      </Text>
      <select
        name={name}
        value={value}
        onChange={onChange}
        style={{
          width: '100%',
          height: 40,
          borderRadius: 8,
          border: '1px solid #E5E7EB',
          padding: '8px 12px',
          background: '#fff',
          fontSize: 13,
        }}
      >
        {children}
      </select>
    </div>
  );
};

export default Select;
