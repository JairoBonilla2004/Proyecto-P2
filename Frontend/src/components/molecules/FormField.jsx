import React from 'react';
import Input from '../atoms/Input';

const FormField = React.forwardRef(({ label, error, className = '', ...props }, ref) => {
  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      {label && (
        <label className="text-sm font-medium text-gray-300">
          {label}
        </label>
      )}
      <Input ref={ref} error={error} {...props} />
      {error && (
        <p className="text-xs text-red-500 mt-0.5">{error}</p>
      )}
    </div>
  );
});

FormField.displayName = 'FormField';
export default FormField;
