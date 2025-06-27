import React from 'react';
import { NavLink } from 'react-router-dom';
import '../Css/FootBar.css';

function Footer() {
  return (
    <div className='footBar' id="footer-container">
      <div className='logo-text' id="footer-logo">
        <h1 className='flexi' id="footer-logo-flexi">Flexi</h1>
        <h1 className='ride' id="footer-logo-ride">Ride</h1>
      </div>

      <div className='foot-menu' id="footer-menu">
        <NavLink 
          to="/" 
          className={({ isActive }) => (isActive ? 'active' : '')}
          id="footer-home-link"
        >
          Home
        </NavLink>
        <NavLink 
          to="/cars" 
          className={({ isActive }) => (isActive ? 'active' : '')}
          id="footer-cars-link"
        >
          Cars
        </NavLink>
      </div>

      <div className='foot-user' id="footer-user">
        <span>Follow us on Facebook</span>
      </div>
    </div>
  );
}

export default Footer;