import React, { useState, useEffect } from 'react';
import carImage from '../assets/car.png';
import '../Css/Login.css';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../Utils/AuthContext';
import eyeImage from '../assets/eye-outline.png';

function Login() {
  const { setIsLoggedIn, setUserData } = useAuth();
  const [loginDetails, setLoginDetails] = useState({ email: '', password: '' });
  const [errorMessage, setErrorMessage] = useState('');
  const [showPasswordHint, setShowPasswordHint] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  const [showNotification, setShowNotification] = useState(false);
  const [notificationMessage, setNotificationMessage] = useState('');

  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    // Check for success message passed through state
    if (location.state && location.state.successMessage) {
      setNotificationMessage(location.state.successMessage);
      setShowNotification(true);
    }
  }, [location]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setLoginDetails((prev) => ({
      ...prev,
      [name]: value
    }));
  };

  const handleCloseNotification = () => {
    setShowNotification(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setShowPasswordHint(true);

    try {
      const response = await axios.post(
        'https://j5u3343jca.execute-api.us-east-2.amazonaws.com/api/auth/sign-in',
        loginDetails
      );

      console.log("Login successful:", response.data);
      setIsLoggedIn(true);
      const { username, userId, userImageUrl } = response.data;
      setUserData({ username, userId, userImageUrl });
      console.log(username);
      navigate('/');
    } catch (error) {
      console.error("Login error:", error);
      setErrorMessage(`The password isn't correct. Check it and try again`);
      setShowPasswordHint(false);
    }
  };

  return (
    <div className='login-main-container' id="login-main-container">
      {showNotification && (
        <div className='success-notification-container' id="success-notification-container">
          <div className="notification-content">
            <button className='close-button' onClick={handleCloseNotification} id="close-notification-button">×</button>
            <h2 id="notification-title">Congratulations!</h2>
            <p id="notification-message">You have successfully created your account!</p>
          </div>
        </div>
      )}
      <div className='login-image-container' id="login-image-container">
        <img src={carImage} alt="Image not found" />
      </div>
      <div className='login-container' id="login-container">
        <div className='login-text-container' id="login-text-container">
          <div className='login-text'>
            <h1 id="login-heading">Log in</h1>
            <p id="login-subheading">Glad to see you again</p>
          </div>
        </div>
        <div className='login-feilds' id="login-fields">
          <form onSubmit={handleSubmit}>
            <div className="form-group" id="email-group">
              <label htmlFor="email">Email</label>
              <input
                type="email"
                placeholder='Write your email'
                name='email'
                value={loginDetails.email}
                onChange={handleChange}
                id="email-input"
                required
              />
            </div>
            <div className="form-group" id="password-group">
              <label htmlFor="password">Password</label>
              <div className="password-input-container" id="password-input-container">
                <input
                  type={showPassword ? 'text' : 'password'}
                  placeholder='Write your password'
                  name='password'
                  value={loginDetails.password}
                  onChange={handleChange}
                  id="password-input"
                  required
                />
                <img
                  src={eyeImage}
                  alt="Toggle visibility"
                  className={`eye-icon ${showPassword ? 'active' : ''}`}
                  onClick={() => setShowPassword(!showPassword)}
                  id="eye-icon"
                />
              </div>
              {showPasswordHint && (
                <p id="password-hint">Minimum 8 characters with at least 1 capital letter and 1 digit</p>
              )}
            </div>
            {errorMessage && <p className="error-message" id="login-error-message">{errorMessage}</p>}
            <button type="submit" id="login-button">Login</button>
          </form>
          <div id="registration-link">
            <h1 className='new-here' id="new-here">New here?</h1>
            <Link className='create-an-account' to='/register' id="create-account-link">Create an account</Link>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;