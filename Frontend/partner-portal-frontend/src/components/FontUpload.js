import React, { useState, useRef, useEffect } from 'react';
import { Button, Icon, Text, InputFieldV2 } from '../custom-components';
import { IcUpload, IcClose, IcSuccess } from '../custom-components/Icon';
import CustomToast from './CustomToastContainer';
import { toast } from 'react-toastify';
import Select from './Select';

const FontUpload = ({ fontName, setFontName, fontBase64, setFontBase64, fontInputRef, fontStyles, setFontStyles }) => {
  const handleFontClick = () => {
    fontInputRef.current.click();
  };

  const handleFontChange = (e) => {
    if (e.target.files.length > 0) {
      const file = e.target.files[0];
      const allowedExtensions = [".woff", ".woff2", ".ttf", ".otf"];
      const fileExtension = file.name.split('.').pop().toLowerCase();
      if (allowedExtensions.includes(`.${fileExtension}`)) {
        setFontName(file.name);
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => {
          const base64Content = reader.result.split(',')[1];
          setFontBase64(base64Content);
          console.log(base64Content)
          setFontStyles(prev => ({ ...prev, url: base64Content, family: file.name.split('.')[0] }));
        };
      } else {
        toast.error(
          <CustomToast
            type="error"
            message="Invalid font file type. Please use .woff, .woff2, .ttf, or .otf"
          />
        );
      }
    }
  };

  const handleRemoveFont = () => {
    if (fontInputRef.current) {
      fontInputRef.current.value = "";
    }
    setFontName("");
    setFontBase64("");
    setFontStyles({
      url: '',
      family: '',
      size: '14px',
      weight: '400',
      style: 'normal'
    });
  };

  const handleStyleChange = (e) => {
    const { name, value } = e.target;
    setFontStyles(prev => ({ ...prev, [name]: value }));
  };

  const handleSizeChange = (e) => {
    const { value } = e.target;
    const validSize = /^\d+(\.\d+)?(px|em|rem|%)$/i.test(value);
    if (validSize) {
      setFontStyles(prev => ({ ...prev, size: value }));
    } else {
      toast.error(
        <CustomToast
          type="error"
          message="Invalid font size. Please use a valid CSS font-size (e.g., 14px, 1.2em)."
        />
      );
    }
  };

  useEffect(() => {
    if (fontBase64 && fontStyles?.family) {
      const styleId = 'custom-font-style';
      let style = document.getElementById(styleId);
      if (!style) {
        style = document.createElement("style");
        style.id = styleId;
        document.head.appendChild(style);
      }

      // Check if it already has data URI prefix
      const fontSrc = fontBase64.startsWith("data:")
        ? fontBase64
        : `data:application/octet-stream;base64,${fontBase64}`;

      style.innerHTML = `
        @font-face {
          font-family: '${fontStyles.family}';
          src: url(${fontSrc});
        }
      `;

      return () => {
        const styleTag = document.getElementById(styleId);
        if (styleTag) {
          // styleTag.remove();
        }
      };
    }
  }, [fontBase64, fontStyles?.family]);

  return (
    <div>
      <div className="language-heading-upload">
        <Text appearance="heading-xxs" color="primary-grey-80">
          Upload custom font
        </Text>
      </div>

      <div
        style={{
          padding: "0px 15px",
          marginBottom: "20px",
        }}
      >
        <div
          className="fileUploader-custom1"
          onClick={handleFontClick}
        >
          <input
            type="file"
            ref={fontInputRef}
            style={{ display: "none" }}
            onChange={handleFontChange}
            accept=".woff,.woff2,.ttf,.otf"
          />

          <div className="flex items-center justify-center">
            <Icon ic={<IcUpload height={23} width={23} />} color="primary_60" />
            <Text appearance="button" color="primary-60">
              Upload Font
            </Text>
          </div>
        </div>

        <Text appearance="body-xs" color="primary-grey-80">
          Upload a font file to preview. Supported formats: .woff, .woff2, .ttf, .otf.
        </Text>

        {fontName && (
          <div className="systemConfiguration-file-uploader">
            <div className="previewFile">
              <Icon
                ic={<IcSuccess width={15} height={15} />}
                color="feedback_success_50"
              />
              <Text appearance="body-xs" color="primary-grey-80">
                {fontName}
              </Text>
            </div>
            <Icon
              ic={<IcClose width={15} height={15} />}
              color="primary_60"
              className="selecetd-file-display"
              onClick={handleRemoveFont}
            />
          </div>
        )}
      </div>

      <div style={{ padding: "0px 15px", marginBottom: "20px" }}>
        <InputFieldV2
          label="Font Family"
          name="family"
          value={fontStyles?.family || ''}
          onChange={handleStyleChange}
        />
        <InputFieldV2
          label="Font Size"
          name="size"
          value={fontStyles?.size || '14px'}
          onChange={handleSizeChange}
        />
        <Select
          label="Font Weight"
          name="weight"
          value={fontStyles?.weight || '400'}
          onChange={(e) => setFontStyles(prev => ({ ...prev, weight: e.target.value }))}
        >
          <option value="100">100</option>
          <option value="200">200</option>
          <option value="300">300</option>
          <option value="400">400 (normal)</option>
          <option value="500">500</option>
          <option value="600">600</option>
          <option value="700">700 (bold)</option>
          <option value="800">800</option>
          <option value="900">900</option>
          <option value="normal">Normal</option>
          <option value="bold">Bold</option>
        </Select>
        <Select
          label="Font Style"
          name="style"
          value={fontStyles?.style || 'normal'}
          onChange={(e) => setFontStyles(prev => ({ ...prev, style: e.target.value }))}
        >
          <option value="normal">Normal</option>
          <option value="italic">Italic</option>
          <option value="oblique">Oblique</option>
        </Select>
      </div>
    </div>
  );
};

export default FontUpload;