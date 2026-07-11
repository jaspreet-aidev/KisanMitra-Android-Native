import tensorflow as tf
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from tensorflow.keras.applications import MobileNetV2
from tensorflow.keras.layers import Dense, GlobalAveragePooling2D, Dropout
from tensorflow.keras.models import Model
from tensorflow.keras import regularizers
from tensorflow.keras.callbacks import EarlyStopping

# 1. Aggressive Data Augmentation
train_datagen = ImageDataGenerator(
    rescale=1./255,
    rotation_range=40,
    width_shift_range=0.2,
    height_shift_range=0.2,
    shear_range=0.2,
    zoom_range=0.2,
    horizontal_flip=True,
    fill_mode='nearest'
)

# Assume your dataset directory is 'dataset/'
train_generator = train_datagen.flow_from_directory(
    'dataset/',
    target_size=(224, 224),
    batch_size=32,
    class_mode='categorical'
)

# 2. Hardened Architecture with L2 Regularization and Dropout
base_model = MobileNetV2(weights='imagenet', include_top=False, input_shape=(224, 224, 3))
base_model.trainable = False  # Keep pre-trained weights frozen initially

x = base_model.output
x = GlobalAveragePooling2D()(x)
# Added L2 regularization and high dropout to force generalization
x = Dense(256, activation='relu', kernel_regularizer=regularizers.l2(0.01))(x)
x = Dropout(0.5)(x) 
predictions = Dense(train_generator.num_classes, activation='softmax')(x)

model = Model(inputs=base_model.input, outputs=predictions)

# 3. Compilation with low learning rate
model.compile(optimizer=tf.keras.optimizers.Adam(learning_rate=0.0001),
              loss='categorical_crossentropy',
              metrics=['accuracy'])

# 4. Training with EarlyStopping to prevent memorization
early_stopping = EarlyStopping(monitor='val_loss', patience=5, restore_best_weights=True)

# Execute Training
history = model.fit(
    train_generator,
    epochs=50, # Set high, but EarlyStopping will kill it if it stalls
    callbacks=[early_stopping]
)

####for having final epoch and graph readings

import matplotlib.pyplot as plt

# Step 1: Audit the available keys
available_keys = history.history.keys()
print(f"System Audit - Found metrics: {available_keys}")

# Step 2: Extract Training Metrics (These always exist)
acc = history.history.get('accuracy', history.history.get('acc', []))
loss = history.history['loss']
epochs = range(1, len(acc) + 1)

# Step 3: Set up the Canvas
plt.figure(figsize=(14, 5))

# --- Graph 1: ACCURACY ---
plt.subplot(1, 2, 1)
plt.plot(epochs, acc, 'b-', label='Training Accuracy', linewidth=2)

# Defensive Check: Only plot validation if it actually exists
if 'val_accuracy' in available_keys:
    plt.plot(epochs, history.history['val_accuracy'], 'darkorange', label='Validation Accuracy', linewidth=2)
elif 'val_acc' in available_keys:
    plt.plot(epochs, history.history['val_acc'], 'darkorange', label='Validation Accuracy', linewidth=2)

plt.title('MobileNetV2: Accuracy over Epochs', fontweight='bold')
plt.xlabel('Epochs')
plt.ylabel('Accuracy')
plt.legend()
plt.grid(True, linestyle='--', alpha=0.6)

# --- Graph 2: LOSS ---
plt.subplot(1, 2, 2)
plt.plot(epochs, loss, 'b-', label='Training Loss', linewidth=2)

# Defensive Check: Only plot validation if it actually exists
if 'val_loss' in available_keys:
    plt.plot(epochs, history.history['val_loss'], 'darkorange', label='Validation Loss', linewidth=2)

plt.title('MobileNetV2: Loss over Epochs', fontweight='bold')
plt.xlabel('Epochs')
plt.ylabel('Loss')
plt.legend()
plt.grid(True, linestyle='--', alpha=0.6)

# Render the dashboard
plt.tight_layout()
plt.show()
