import React, { useState, useEffect } from 'react';
import '../Css/AboutUs.css';
import axios from 'axios';

function AboutUs() {
  const [aboutUsData, setAboutUsData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchAboutUsData = async () => {
      try {
        const response = await axios.get('https://j5u3343jca.execute-api.us-east-2.amazonaws.com/api/home/about-us');
        setAboutUsData(response.data.content); // Set the content directly
      } catch (error) {
        console.error('Error fetching the About Us data: ', error);
        setError('Could not fetch data. Please try again later.');
      } finally {
        setLoading(false); // Stop loading regardless of the outcome
      }
    };

    fetchAboutUsData();
  }, []);

  // If data is still loading or if there is an error, show a loader or error message
  if (loading) {
    return <div>Loading...</div>;
  }

  if (error) {
    return <div>{error}</div>; // Display error message if there's an error
  }

  return (
    <div className="about-us-container" id="about-us-container">
      <div className="about-us-grid" id="about-us-grid">
        <div className="about-us-column" id="about-us-title-column">
          <div className="about-us-row about-us-title" id="about-us-title">(ABOUT US)</div>
        </div>

        <div className="about-us-column" id="about-us-years-column">
          <div className="about-us-row about-us-header" id="about-us-years-header">Years</div>
          <div className="about-us-row about-us-number" id="about-us-years-number">
            {aboutUsData.find(item => item.title === 'years')?.numericValue || 'N/A'}
          </div>
          <div className="about-us-row about-us-description" id="about-us-years-description">
            {aboutUsData.find(item => item.title === 'years')?.description || 'N/A'}
          </div>
        </div>

        <div className="about-us-column" id="about-us-locations-column">
          <div className="about-us-row about-us-header" id="about-us-locations-header">Locations</div>
          <div className="about-us-row about-us-number" id="about-us-locations-number">
            {aboutUsData.find(item => item.title === 'locations')?.numericValue || 'N/A'}
          </div>
          <div className="about-us-row about-us-description" id="about-us-locations-description">
            {aboutUsData.find(item => item.title === 'locations')?.description || 'N/A'}
          </div>
        </div>

        <div className="about-us-column" style={{ visibility: 'hidden' }} id="about-us-empty-column">
          <div className="about-us-row"></div>
          <div className="about-us-row"></div>
          <div className="about-us-row"></div>
        </div>

        <div className="about-us-column" id="about-us-car-brands-column">
          <div className="about-us-row about-us-header" id="about-us-car-brands-header">Car Brands</div>
          <div className="about-us-row about-us-number" id="about-us-car-brands-number">
            {aboutUsData.find(item => item.title === 'car brands')?.numericValue || 'N/A'}
          </div>
          <div className="about-us-row about-us-description" id="about-us-car-brands-description">
            {aboutUsData.find(item => item.title === 'car brands')?.description || 'N/A'}
          </div>
        </div>

        <div className="about-us-column" id="about-us-cars-column">
          <div className="about-us-row about-us-header" id="about-us-cars-header">Cars</div>
          <div className="about-us-row about-us-number" id="about-us-cars-number">
            {aboutUsData.find(item => item.title === 'cars')?.numericValue || 'N/A'}
          </div>
          <div className="about-us-row about-us-description" id="about-us-cars-description">
            {aboutUsData.find(item => item.title === 'cars')?.description || 'N/A'}
          </div>
        </div>
      </div>
    </div>
  );
}

export default AboutUs;