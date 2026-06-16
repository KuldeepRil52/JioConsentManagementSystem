import React, { useState, useEffect, useMemo } from 'react';
import { Text, ActionButton, Icon } from '../custom-components';
import { IcEditPen } from '../custom-components/Icon';
import React, { useState, useEffect, useCallback } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { Text, ActionButton, Icon, Button } from '../custom-components';
import { IcEditPen } from '../custom-components/Icon';
import { useNavigate } from 'react-router-dom';
import AddCategoryModal from './AddCategoryModal';
import EditCategoryModal from './EditCategoryModal';
import '../styles/cookieCategory.css';
import '../styles/dataProtectionOfficer.css';
import '../styles/pageConfiguration.css';
import '../styles/masterSetup.css';
import { languages } from '../utils/languages';
import { getCookieCategories, createCookieCategory, updateCookieCategory, getTranslations } from '../store/actions/CommonAction';
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

const CookieCategory = () => {
  const [showAddCategoryModal, setShowAddCategoryModal] = useState(false);
  const [showEditCategoryModal, setShowEditCategoryModal] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [loading, setLoading] = useState(false);
  const [selectedLanguage, setSelectedLanguage] = useState(languages[0].value);
  const [categories, setCategories] = useState([]);
  const dispatch = useDispatch();
  const multilingualConfigSaved = useSelector((state) => state.common.multilingualConfigSaved);
  const navigate = useNavigate();
  const [showMultilingualWarningModal, setShowMultilingualWarningModal] = useState(false);

  const handleLanguageChange = (e) => {
    const selectedLang = e.target.value;
    
    // Allow English without config check
    if (selectedLang === "en") {
      setSelectedLanguage(selectedLang);
      return;
    }
    
    // Check if multilingual config is saved
    if (!multilingualConfigSaved) {
      setShowMultilingualWarningModal(true);
      // Reset dropdown to previous value
      e.target.value = selectedLanguage || "en";
      return;
    }
    
    setSelectedLanguage(selectedLang);
  };

  const fetchCategories = useCallback(async () => {
    setLoading(true);
    try {
      const response = await dispatch(getCookieCategories());
      // Handle both API response structures: direct array or object with searchList
      let categoriesData = [];
      if (Array.isArray(response.data)) {
        categoriesData = response.data;
      } else if (response.data && Array.isArray(response.data.searchList)) {
        categoriesData = response.data.searchList;
      } else if (response.data && Array.isArray(response.searchList)) {
        categoriesData = response.searchList;
      }
      setCategories(categoriesData);
    } catch (error) {
      console.error("Error fetching categories:", error);
      setCategories([]);
    }
    setLoading(false);
  }, [dispatch]);

  useEffect(() => {
    fetchCategories();
  }, [fetchCategories]);

  const [translatedCategories, setTranslatedCategories] = useState([]);

  useEffect(() => {
    const translateCategories = async () => {
      // Ensure categories is an array
      if (!Array.isArray(categories)) {
        setTranslatedCategories([]);
        return;
      }
      
      if (selectedLanguage === 'en') {
        setTranslatedCategories(categories);
        return;
      }
      if (categories.length === 0) {
        setTranslatedCategories([]);
        return;
      }
      
      setLoading(true);
      try {
        // Use index as the primary unique identifier to ensure uniqueness
        const textsToTranslate = categories.flatMap((cat, index) => {
          // Use index as the unique identifier to ensure each category gets unique translation IDs
          return [
            { id: `cat-${index}-category`, source: cat.category },
            { id: `cat-${index}-desc`, source: cat.description },
          ];
        });
        
        const translationResult = await dispatch(getTranslations(textsToTranslate, selectedLanguage, 'en'));
        
        if (translationResult && translationResult.output && Array.isArray(translationResult.output)) {
          const translatedData = categories.map((cat, index) => {
            const categoryTranslation = translationResult.output.find(t => t.id === `cat-${index}-category`);
            const descTranslation = translationResult.output.find(t => t.id === `cat-${index}-desc`);
            
            const translatedName = categoryTranslation?.target || cat.category;
            const translatedDesc = descTranslation?.target || cat.description;
            
            return { ...cat, category: translatedName, description: translatedDesc };
          });
          setTranslatedCategories(translatedData);
        } else {
          // If translation fails or returns unexpected structure, use original categories
          console.warn('Translation result has unexpected structure, using original categories');
          setTranslatedCategories(categories);
        }
      } catch (error) {
        console.error('Error translating categories:', error);
        // On error, use original categories
        setTranslatedCategories(categories);
      } finally {
        setLoading(false);
      }
    };
    
    translateCategories();
  }, [selectedLanguage, categories, dispatch]);

  const handleEditClick = (category) => {
    setSelectedCategory(category);
    setShowEditCategoryModal(true);
  };

  const handleAddCategory = async (name, description) => {
    try {
      await dispatch(createCookieCategory(name, description));
      setShowAddCategoryModal(false);
      fetchCategories();
      toast.success(
        (props) => (
          <CustomToast
            {...props}
            type="success"
            message={"Category created successfully!"}
          />
        ),
        { icon: false }
      );
    } catch (error) {
      console.error("Error creating category:", error);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={error.message || "Failed to create category."}
          />
        ),
        { icon: false }
      );
    }
  };

  const handleUpdateCategory = async (name, description) => {
    try {
      await dispatch(updateCookieCategory(name, description));
      setShowEditCategoryModal(false);
      fetchCategories();
      toast.success(
        (props) => (
          <CustomToast
            {...props}
            type="success"
            message={"Category updated successfully!"}
          />
        ),
        { icon: false }
      );
    } catch (error) {
      console.error("Error updating category:", error);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={error.message || "Failed to update category."}
          />
        ),
        { icon: false }
      );
    }
  };

  return (
    <div className="configurePage">
      <div className="dataProtectionOfficer-outer-division">
        <div className="dataProtectionOfficer-header-and-badge">
            <Text appearance="heading-s" color="primary-grey-100">Manage Categories</Text>
            <div className="dataProtectionOfficer-badge" style={{ marginTop: '5px' }}>
              <Text appearance="body-xs-bold" color="primary-grey-80">Cookies</Text>
            </div>
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginTop: '20px' }}>
          <div className="language-selector-container" style={{ display: 'flex', flexDirection: 'column' }}>
            <Text appearance="body-s">Select language to view and edit translation</Text>
            <select
              value={selectedLanguage}
              onChange={handleLanguageChange}
              style={{
                width: '320px',
                padding: '8px',
                borderRadius: '6px',
                border: '1px solid #ccc',
                marginTop: '8px',
              }}
            >
              {languages.map((lang) => (
                <option key={lang.value} value={lang.value}>{lang.label}</option>
              ))}
            </select>
          </div>
          <div className="add-category-btn" style={{ marginRight: '5%' }}>
            <ActionButton label="Add category" kind="primary" onClick={() => setShowAddCategoryModal(true)}></ActionButton>
          </div>
        </div>
        <div className="table-container">
          {loading ? (
            <Text>Loading...</Text>
          ) : (
            <table className="custom-table">
              <thead>
                <tr>
                  <th><Text appearance="body-s-bold" color="primary-grey-80">Category name</Text></th>
                  <th><Text appearance="body-s-bold" color="primary-grey-80">Description</Text></th>
                  <th><Text appearance="body-s-bold" color="primary-grey-80">Action</Text></th>
                </tr>
              </thead>
              <tbody>
                {Array.isArray(translatedCategories) && translatedCategories.map((category, index) => (
                  <tr key={index}>
                    <td data-label="Category name"><Text appearance="body-s-bold" color="black">{category.category}</Text></td>
                    <td data-label="Description"><Text appearance="body-s" color="black">{category.description}</Text></td>
                    <td data-label="Action"><Icon ic={<IcEditPen height={24} width={24} />} color="primary_grey_80" kind="default" size="medium" onClick={() => handleEditClick(category)} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        <div className="common-add-btn cookie-category-save-btn">
          <ActionButton label="Save" kind="primary"></ActionButton>
        </div>
      </div>
      {showAddCategoryModal && <AddCategoryModal onClose={() => setShowAddCategoryModal(false)} onAdd={handleAddCategory} />}
      {showEditCategoryModal && <EditCategoryModal onClose={() => setShowEditCategoryModal(false)} category={selectedCategory} onUpdate={handleUpdateCategory} />}

      {/* Multilingual Warning Modal */}
      {showMultilingualWarningModal && (
        <div className="modal-overlay" onClick={() => setShowMultilingualWarningModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px' }}>
            <div className="modal-header">
              <h2 style={{ margin: 0, fontSize: '20px', fontWeight: '600' }}>
                Multilingual Support Required
              </h2>
              <button
                className="modal-close-btn"
                onClick={() => setShowMultilingualWarningModal(false)}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '24px',
                  cursor: 'pointer',
                  color: '#666',
                }}
              >
                ×
              </button>
            </div>
            
            <div className="modal-body">
              <Text appearance="body-s" color="primary-grey-80" style={{ marginBottom: '24px' }}>
                You need to configure multilingual support in the consent configuration to enable the translate functionality.
              </Text>

              <div style={{
                display: 'flex',
                gap: '12px',
                justifyContent: 'flex-end',
              }}>
                <Button
                  kind="secondary"
                  size="medium"
                  label="Cancel"
                  onClick={() => setShowMultilingualWarningModal(false)}
                />
                <ActionButton
                  kind="primary"
                  size="medium"
                  label="Go to Configuration"
                  onClick={() => {
                    setShowMultilingualWarningModal(false);
                    navigate("/consent");
                  }}
                />
              </div>
            </div>
          </div>
        </div>
      )}

      <ToastContainer
        position="bottom-left"
        autoClose={3000}
        hideProgressBar
        closeOnClick
        pauseOnHover
        draggable
        closeButton={false}
        toastClassName={() => "toast-wrapper"}
        transition={Slide}
      />
    </div>
  );
};

export default CookieCategory;