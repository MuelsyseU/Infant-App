
# def main():
#     return "whats"

import os
import importlib
import threading

def resolve_path(org):
    return os.path.join(os.path.dirname(__file__), org)

# 控制输入音频的长度
max_pad_len = 500
num_mfcc = 20
min_confidence = 0.9

# Data Format
num_rows = num_mfcc * 3
num_columns = max_pad_len
num_channels = 1
# NUM_LABELS
num_labels = 6
filter_size = 2

# Model Path
TFLITE_FILE_PATH = resolve_path("ReleaseModels/Copy9.tflite")

# Async Initialization
def thread_import():
    global np, librosa, tflite
    print('begun')
    np = importlib.import_module("numpy")
    librosa = importlib.import_module("librosa")
    tflite = importlib.import_module("tflite_runtime.interpreter")
    print('finished')
def thread_build():
    global interpreter
    interpreter = tflite.Interpreter(TFLITE_FILE_PATH)
    interpreter.allocate_tensors()
def thread_init():
    thread_import()
    thread_build()
def begin_init():
    global foo_thread
    foo_thread = threading.Thread(target=thread_init)
    foo_thread.start()
def wait_init():
    print('wait')
    foo_thread.join()
    print('done')

def extract_features(file_name):
    audio, sample_rate = librosa.load(file_name, res_type='kaiser_fast')
    mfccs = librosa.feature.mfcc(y=audio, sr=sample_rate, n_mfcc=num_mfcc)
    mfccs_delta = librosa.feature.delta(mfccs)
    mfccs_delta2 = librosa.feature.delta(mfccs_delta)
    data = np.concatenate((mfccs, mfccs_delta, mfccs_delta2))
    pad_width = max_pad_len - data.shape[1]
    data = np.pad(data, pad_width=((0, 0), (0, pad_width)), mode='constant')
    return data

def predict(features):
    features = features.reshape(1, num_mfcc * 3, max_pad_len, 1)
    print(features)
    # output = model.predict(x = features.reshape(1, num_mfcc * 3, max_pad_len, 1))
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    interpreter.set_tensor(input_details[0]['index'], features)
    interpreter.invoke()
    output = interpreter.get_tensor(output_details[0]['index'])
    print(output)
    return np.argmax(output) if np.max(output) > min_confidence else -1

def solve(file_name):
    wait_init()
    try:
        return predict(extract_features(file_name))
    except Exception as e:
        print("Error encountered while parsing file: ", file_name, e)
        return -2
