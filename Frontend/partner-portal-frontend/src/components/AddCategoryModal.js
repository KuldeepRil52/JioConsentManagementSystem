import React, { useState } from 'react';
import { Text, ActionButton, Icon, InputFieldV2 } from '../custom-components';
import { IcClose, IcChevronRight, IcChevronLeft } from '../custom-components/Icon';
import '../styles/addCategoryModal.css';
import '../styles/masterSetup.css';
import { useEffect } from 'react';

import { getTranslations } from '../utils/translationApi';

import { languages } from '../utils/languages';

const AddCategoryModal = ({ onClose,onAdd }) => {
  const [activeLanguage, setActiveLanguage] = useState(languages[0].value);
  const [startIndex, setStartIndex] = useState(0);
  const [cookieName, setCookieName] = useState('');
  const [description, setDescription] = useState('');
    const handleAdd = () => {
    if (cookieName && description) {
      onAdd(cookieName, description);
    }
  };
  
  const visibleLanguages = 6;

  useEffect(() => {
    const translateInputs = async () => {
      if (!cookieName && !description) return;

      const textsToTranslate = [];
      if (cookieName) textsToTranslate.push({ id: 'cookieName', source: cookieName });
      if (description) textsToTranslate.push({ id: 'description', source: description });

      if (textsToTranslate.length === 0) return;

      try {
        const translationResult = await getTranslations(textsToTranslate, activeLanguage);

        // Check if translationResult and output exist
        if (!translationResult || !translationResult.output || !Array.isArray(translationResult.output)) {
          console.error('Invalid translation result structure:', translationResult);
          // Keep original values if translation fails
          return;
        }

        const translatedName = translationResult.output.find(t => t.id === 'cookieName')?.target || cookieName;
        const translatedDesc = translationResult.output.find(t => t.id === 'description')?.target || description;

        setCookieName(translatedName);
        setDescription(translatedDesc);
      } catch (error) {
        console.error('Error translating inputs:', error);
        // Keep original values if translation fails
      }
    };

    // We only translate if the language is not English
    if (activeLanguage !== 'en') {
      translateInputs();
    }
  }, [activeLanguage]);

  const handleNext = () => {
    if (startIndex + visibleLanguages < languages.length) {
      setStartIndex(startIndex + 1);
    }
  };

  const handlePrev = () => {
    if (startIndex > 0) {
      setStartIndex(startIndex - 1);
    }
  };

  return (
    <div className="modal-outer-container">
      <div className="master-set-up-modal-container">
        <div className="modal-close-btn-container">
          <ActionButton onClick={onClose} icon={<IcClose />} kind="tertiary" />
        </div>
        <Text appearance="heading-xs">Add cookie category</Text>
        <div className="language-tabs-container">
          <div
            className="arrow-btn"
            onClick={handlePrev}
            disabled={startIndex === 0}
          >
            <Icon ic={<IcChevronLeft />} color="#0f3cc9" />
          </div>
          <div className="language-tabs">
            {languages.slice(startIndex, startIndex + visibleLanguages).map(lang => (
              <div
                key={lang.value}
                className={`language-tab ${activeLanguage === lang.label ? 'active' : ''}`}
                onClick={() => setActiveLanguage(lang.value)}
              >
                <Text appearance={activeLanguage === lang.label ? "body-s-bold" : "body-s"}>{lang.label}</Text>
              </div>
            ))}
          </div>
          <div
            className="arrow-btn"
            onClick={handleNext}
            disabled={startIndex + visibleLanguages >= languages.length}
          >
            <Icon ic={<IcChevronRight />} color="#0f3cc9" />
          </div>
        </div>
        <div className="form-container">
          <InputFieldV2 label="Cookie name" value={cookieName} onChange={(e) => setCookieName(e.target.value)} />
          <div className="description-container">
            <Text as="label" htmlFor="description">Description</Text>
            <textarea id="description" rows="4" value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>
        </div>
        <div className="modal-add-btn-container">
          <ActionButton label="Add" kind="primary"  onClick={handleAdd} />
        </div>
      </div>
    </div>
  );
};

export default AddCategoryModal;
