import React from 'react';
import '../Css/Faq.css';

function Faq() {
    return (
        <div className="faq-container" id="faq-container">
            <div className='text-container' id="text-container">
                <h3 id="faq-title">(FAQ)</h3>
            </div>
            <div className="faq-grid" id="faq-grid">
                <div className="faq-container-left" id="faq-container-left">
                    {/* This container is intentionally left empty */}
                </div>
                <div className="faq-container-right" id="faq-container-right">
                    <div className="faq-row" id="faq-row-1">
                        <h4>What documents do I need to rent a car?</h4>
                        <p>To rent a car, you will need a valid driver's license, a credit card in your name, and a government-issued photo ID (such as a passport or national ID). International renters may also need to present an International Driving Permit (IDP) in addition to their home country's driver’s license.</p>
                    </div>
                    <div className="faq-row" id="faq-row-2">
                        <h4>Is there an age requirement to rent a car?</h4>
                        <p>Yes, most rental companies require drivers to be at least 21 years old. Additional fees might apply for drivers under 25.</p>
                    </div>
                    <div className="faq-row" id="faq-row-3">
                        <h4>What should I do if the rental car breaks down or I get into an accident?</h4>
                        <p>If you experience a breakdown, you should contact the rental company. In case of an accident, ensure everyone's safety and report the incident to the authorities and the rental company immediately.</p>
                    </div>
                    <div className="faq-row" id="faq-row-4">
                        <h4>Is there a mileage limit on my rental?</h4>
                        <p>This often depends on the rental agreement. Some contracts have unlimited mileage, while others set a certain limit, after which additional charges may apply.</p>
                    </div>
                    <div className="faq-row" id="faq-row-5">
                        <h4>Can I extend my rental period?</h4>
                        <p>Yes, you can usually extend your rental period by contacting the rental company, but availability and additional fees may apply.</p>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Faq;