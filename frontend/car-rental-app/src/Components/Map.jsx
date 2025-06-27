import React from 'react';
import '../Css/Map.css'; 
import map from '../assets/Map.png';

function Map() {
  return (
    <div className="map-wrapper" id="map-wrapper">
      <h2 className="map-heading" id="map-heading">(Our Locations)</h2>

      <div className="map-container" id="map-container">
        {/* First Column */}
        <div className="map-column-left" id="map-column-left">
          <div className="map-row" id="map-row-1">Kyiv Hayatt Hotel</div>
          <div className="map-row" id="map-row-2">Kyiv Hayatt Hotel</div>
          <div className="map-row" id="map-row-3">Kyiv Hayatt Hotel</div>
          <div className="map-row" id="map-row-4">Kyiv Hayatt Hotel</div>
          <div className="map-row" id="map-row-5">Kyiv Hayatt Hotel</div>
          <div className="map-row" id="map-row-6">Kyiv Hayatt Hotel</div>
          <div className="map-row" id="map-row-7">Kyiv Hayatt Hotel</div>
        </div>

        {/* Second Column */}
        <div className="map-column-right" id="map-column-right">
          <img src={map} alt="Map" className="map-image" id="map-image" />
        </div>
      </div>
    </div>
  );
}

export default Map;