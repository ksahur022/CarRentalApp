import React, { useState } from 'react';
import carImage from '../assets/car.png';
import eyeImage from '../assets/eye-outline.png';
import '../Css/Registration.css';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';

function Registration() {
    const [userDetails, setUserDetails] = useState({
        firstName: '',
        lastName: '',
        email: '',
        password: '',
        confirmPassword: ''
    });

    const [errors, setErrors] = useState({});
    const [submitted, setSubmitted] = useState(false);
    const [apiErrorEmail, setApiErrorEmail] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [isRegistering, setIsRegistering] = useState(false); // New loading state
    const navigate = useNavigate();

    const handleChange = (e) => {
        const { name, value } = e.target;
        setUserDetails(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const validate = () => {
        const errs = {};
        const { firstName, lastName, password, confirmPassword } = userDetails;
        const nameRegex = /^[A-Za-z'-]{1,50}$/;

        if (!nameRegex.test(firstName)) {
            errs.firstName = "First name must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed.";
        }

        if (!nameRegex.test(lastName)) {
            errs.lastName = "Last name must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed.";
        }

        if (!/.{8,16}/.test(password)) {
            errs.password = "Password must be 8-16 characters long.";
        } else if (!/[A-Z]/.test(password)) {
            errs.password = "Password must include at least one uppercase letter.";
        } else if (!/[a-z]/.test(password)) {
            errs.password = "Password must include at least one lowercase letter.";
        } else if (!/[0-9]/.test(password)) {
            errs.password = "Password must include at least one number.";
        } else if (!/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
            errs.password = "Password must include at least one special character.";
        }

        if (confirmPassword !== password) {
            errs.confirmPassword = "Password should match.";
        }

        return errs;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitted(true);
        setApiErrorEmail('');
        const validationErrors = validate();
        setErrors(validationErrors);
        
        if (Object.keys(validationErrors).length === 0) {
            setIsRegistering(true);
            try {
                const { confirmPassword, ...userDataToSend } = userDetails;
                const response = await axios.post(
                    'https://j5u3343jca.execute-api.us-east-2.amazonaws.com/api/auth/sign-up',
                    userDataToSend
                );
                navigate('/login', { state: { successMessage: 'Congratulations! You have successfully created your account!' } });
                console.log("Registration successful:", response.data);
            } catch (error) {
                console.error("Registration error:", error);
                if (error.response && error.response.status === 409) {
                    setApiErrorEmail('This email is already registered.');
                } else {
                    setApiErrorEmail('Something went wrong. Please try again later.');
                }
            } finally {
                setIsRegistering(false);
            }
        }
    };

    return (
        <div className="registration-main-container" id="registration-main-container">
            <div className="registration-image-container" id="registration-image-container">
                <img src={carImage} alt="image not found" />
            </div>
            <div className="registration-container" id="registration-container">
                <div className="registration-text-container" id="registration-text-container">
                    <div className="registration-text single-line">
                        <h1 id="registration-heading">Create an account</h1>
                        <p id="registration-subheading">Enter your details below to get started</p>
                    </div>
                </div>
                <div className="registration-fields" id="registration-fields">
                    <form onSubmit={handleSubmit} id="registration-form">
                        <div className="form-row" id="form-row">
                            <div className="form-group half-width" id="first-name-group">
                                <label htmlFor="firstName">Name</label>
                                <input
                                    type="text"
                                    placeholder="Write your name"
                                    name="firstName"
                                    value={userDetails.firstName}
                                    onChange={handleChange}
                                    id="first-name-input"
                                />
                                {submitted && errors.firstName && (
                                    <p className="error" id="first-name-error">{errors.firstName}</p>
                                )}
                            </div>
                            <div className="form-group half-width" id="last-name-group">
                                <label htmlFor="lastName">Surname</label>
                                <input
                                    type="text"
                                    placeholder="Write your surname"
                                    name="lastName"
                                    value={userDetails.lastName}
                                    onChange={handleChange}
                                    id="last-name-input"
                                />
                                {submitted && errors.lastName && (
                                    <p className="error" id="last-name-error">{errors.lastName}</p>
                                )}
                            </div>
                        </div>
                        <div className="form-group" id="email-group">
                            <label htmlFor="email">Email</label>
                            <input
                                type="email"
                                placeholder="Write your email"
                                name="email"
                                value={userDetails.email}
                                onChange={handleChange}
                                id="email-input"
                            />
                            {apiErrorEmail && (
                                <p className="error" id="email-error">{apiErrorEmail}</p>
                            )}
                        </div>
                        <div className="form-group" id="password-group">
                            <label htmlFor="password">Password</label>
                            <div className="password-container" id="password-container">
                                <input
                                    type={showPassword ? 'text' : 'password'}
                                    placeholder="Create password"
                                    name="password"
                                    value={userDetails.password}
                                    onChange={handleChange}
                                    id="password-input"
                                />
                                <img
                                    src={eyeImage}
                                    alt="Toggle visibility"
                                    className={`eye-icon ${showPassword ? 'active' : ''}`}
                                    onClick={() => setShowPassword(!showPassword)}
                                    id="password-eye-icon"
                                />
                            </div>
                            {submitted && errors.password && (
                                <p className="error" id="password-error">{errors.password}</p>
                            )}
                        </div>

                        <div className="form-group" id="confirm-password-group">
                            <label htmlFor="confirmPassword">Confirm Password</label>
                            <div className="password-container" id="confirm-password-container">
                                <input
                                    type={showConfirmPassword ? 'text' : 'password'}
                                    placeholder="Confirm password"
                                    name="confirmPassword"
                                    value={userDetails.confirmPassword}
                                    onChange={handleChange}
                                    id="confirm-password-input"
                                />
                                <img
                                    src={eyeImage}
                                    alt="Toggle visibility"
                                    className={`eye-icon ${showConfirmPassword ? 'active' : ''}`}
                                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                    id="confirm-password-eye-icon"
                                />
                            </div>
                            {submitted && errors.confirmPassword && (
                                <p className="error" id="confirm-password-error">{errors.confirmPassword}</p>
                            )}
                        </div>

                        <div className="button-group" id="button-group">
                            <Link to="/" className="back-button" id="cancel-button">Cancel</Link>
                            <button type="submit" className="submit-button" id="register-button" disabled={isRegistering}>
                                {isRegistering ? 'Registering...' : 'Register'}
                            </button>
                        </div>

                        <p className="login-redirect" id="login-redirect">
                            Already have an account? <Link to="/login" id="login-link">Login</Link>
                        </p>
                    </form>
                </div>
            </div>
        </div>
    );
}

export default Registration;