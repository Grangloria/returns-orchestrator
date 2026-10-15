import React from 'react';
import { ReturnWizardProvider, useReturnWizard } from './context/ReturnWizardContext';
import { Step1OrderLookup } from './components/Step1OrderLookup';
import { Step2ItemSelection } from './components/Step2ItemSelection';
import { Step3EvidenceUpload } from './components/Step3EvidenceUpload';
import { Step4Summary } from './components/Step4Summary';

const WizardContainer: React.FC = () => {
  const { state } = useReturnWizard();

  return (
      <div className="min-h-screen bg-gray-100 py-12 px-4">
        <div className="max-w-md mx-auto mb-8">
          <h1 className="text-2xl font-bold text-center text-gray-900">Customer Return Portal</h1>
          <div className="flex justify-between items-center mt-6 px-4">
            {[1, 2, 3, 4].map((stepNum) => (
                <div
                    key={stepNum}
                    className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition-colors ${
                        state.step >= stepNum ? 'bg-blue-600 text-white' : 'bg-gray-300 text-gray-600'
                    }`}
                >
                  {stepNum}
                </div>
            ))}
          </div>
        </div>

        {state.step === 1 && <Step1OrderLookup />}
        {state.step === 2 && <Step2ItemSelection />}
        {state.step === 3 && <Step3EvidenceUpload />}
        {state.step === 4 && <Step4Summary />}
      </div>
  );
};

export default function App() {
  return (
      <ReturnWizardProvider>
        <WizardContainer />
      </ReturnWizardProvider>
  );
}