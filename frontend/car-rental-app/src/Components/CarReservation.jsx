import React from "react";
import "../Css/CarReservation.css"; // Assuming the CSS is in this file
import carImage from "../assets/cardCar.png"; // Replace with the actual car image path
import { useAuth } from "../Utils/AuthContext";
import NavBar from "./NavBar";

function CarReservation() {
    const { userData } = useAuth();
    return (
        <>
        <NavBar/>
            <div className="car-booking-page">
                <h1 className="page-title">Car booking</h1>

                <div className="booking-container">
                    {/* Left Section */}
                    <div className="left-section">
                        {/* Personal Info */}
                        <div className="info-card">
                            <h3 className="card-title">Personal info</h3>
                            <p className="info-text">{userData.username}</p>
                            <p className="info-text">{ }</p>
                            <p className="info-text">+38 111 111 11 11</p>
                        </div>

                        {/* Location */}
                        <div className="info-card">
                            <h3 className="card-title">Location</h3>
                            <div className="location-row">
                                <p className="info-label">Pick-up location</p>
                                <p className="info-text">Kyiv Hyatt Hotel</p>
                                <button className="change-button">Change</button>
                            </div>
                            <div className="location-row">
                                <p className="info-label">Drop-off location</p>
                                <p className="info-text">Kyiv Hyatt Hotel</p>
                                <button className="change-button">Change</button>
                            </div>
                        </div>

                        {/* Dates & Time */}
                        <div className="info-card">
                            <h3 className="card-title">Dates & Time</h3>
                            <div className="location-row">
                                <p className="info-label">Pick-up date & Time</p>
                                <p className="info-text">November 11 | 10:00</p>
                                <button className="change-button">Change</button>
                            </div>
                            <div className="location-row">
                                <p className="info-label">Drop-off date & Time</p>
                                <p className="info-text">November 16 | 16:00</p>
                                <button className="change-button">Change</button>
                            </div>
                        </div>
                    </div>

                    {/* Right Section */}
                    <div className="right-section">
                        <div className="car-card">
                            <img src={carImage} alt="Car" className="car-image" />
                            <div className="car-details">
                                <h3 className="car-title">Audi A6 Quattro 2023</h3>
                                <p className="car-location">Ukraine, Kyiv</p>
                                <div className="car-pricing">
                                    <p className="total-label">Total</p>
                                    <p className="total-price">$ 900</p>
                                </div>
                                <p className="deposit-text">Deposit: $ 2000</p>
                                <button className="confirm-button">Confirm reservation</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </>

    );
}

export default CarReservation;