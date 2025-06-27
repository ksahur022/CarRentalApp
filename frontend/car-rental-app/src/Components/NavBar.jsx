import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../Utils/AuthContext';
import '../Css/NavBar.css';

function NavBar() {
    const { isLoggedIn, userData } = useAuth();
    const [menuOpen, setMenuOpen] = useState(false); // State to handle menu toggle

    // Function to toggle menu visibility
    const toggleMenu = () => {
        setMenuOpen(!menuOpen);
    };

    return (
        <div className='navBar' id="navbar">
            <div className='logo-text' id="navbar-logo">
                <h1 className='flexi' id="navbar-flexi">Flexi</h1>
                <h1 className='ride' id="navbar-ride">Ride</h1>
            </div>

            {/* Hamburger icon */}
            <div className='hamburger' onClick={toggleMenu}>
                <span></span>
                <span></span>
                <span></span>
            </div>

            <div className={`nav-menu ${menuOpen ? 'show' : ''}`} id="navbar-menu">
                <NavLink
                    to="/"
                    className={({ isActive }) => (isActive ? 'active' : '')}
                    id="navbar-home-link"
                >
                    Home
                </NavLink>
                <NavLink
                    to="/cars"
                    className={({ isActive }) => (isActive ? 'active' : '')}
                    id="navbar-cars-link"
                >
                    Cars
                </NavLink>
                {isLoggedIn && (
                    <NavLink
                        to="/my-bookings"
                        className={({ isActive }) => (isActive ? 'active' : '')}
                        id="navbar-my-bookings-link"
                    >
                        My Bookings
                    </NavLink>
                )}
            </div>

            <div className={`nav-user ${menuOpen ? 'show' : ''}`} id="navbar-user">
                {!isLoggedIn ? (
                    <NavLink to="/login" id="navbar-login-link">Login</NavLink>
                ) : (
                    <span id="navbar-username">Hello, {userData?.username}</span>
                )}
                <span id="navbar-language">En</span>
            </div>
        </div>
    );
}

export default NavBar;
